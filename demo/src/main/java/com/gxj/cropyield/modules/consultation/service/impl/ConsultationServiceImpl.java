package com.gxj.cropyield.modules.consultation.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.auth.service.CurrentUserService;
import com.gxj.cropyield.modules.consultation.dto.ConsultationAttachmentDownload;
import com.gxj.cropyield.modules.consultation.dto.ConsultationAttachmentResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationCreateRequest;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessageRequest;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessageResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessagesResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationPageResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationParticipantResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationSummary;
import com.gxj.cropyield.modules.consultation.dto.ConsultationUpdateRequest;
import com.gxj.cropyield.modules.consultation.entity.Consultation;
import com.gxj.cropyield.modules.consultation.entity.ConsultationAttachment;
import com.gxj.cropyield.modules.consultation.entity.ConsultationMessage;
import com.gxj.cropyield.modules.consultation.entity.ConsultationParticipant;
import com.gxj.cropyield.modules.consultation.repository.ConsultationAttachmentRepository;
import com.gxj.cropyield.modules.consultation.repository.ConsultationMessageRepository;
import com.gxj.cropyield.modules.consultation.repository.ConsultationParticipantRepository;
import com.gxj.cropyield.modules.consultation.repository.ConsultationRepository;
import com.gxj.cropyield.modules.consultation.service.ConsultationService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 在线咨询模块的业务实现类，负责落实咨询会话领域的业务逻辑。
 */
@Service
@Transactional
public class ConsultationServiceImpl implements ConsultationService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_DEPT = "AGRICULTURE_DEPT";
    private static final String ROLE_FARMER = "FARMER";

    private final ConsultationRepository consultationRepository;
    private final ConsultationMessageRepository messageRepository;
    private final ConsultationParticipantRepository participantRepository;
    private final ConsultationAttachmentRepository attachmentRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    private final Path attachmentRoot = Path.of("uploads", "consultations");

    public ConsultationServiceImpl(ConsultationRepository consultationRepository,
                                    ConsultationMessageRepository messageRepository,
                                    ConsultationParticipantRepository participantRepository,
                                    ConsultationAttachmentRepository attachmentRepository,
                                    CurrentUserService currentUserService,
                                    UserRepository userRepository) {
        this.consultationRepository = consultationRepository;
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
        this.attachmentRepository = attachmentRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationPageResponse listConsultations(int page, int pageSize) {
        int pageIndex = Math.max(page - 1, 0);
        int size = pageSize <= 0 ? 20 : pageSize;
        Pageable pageable = PageRequest.of(pageIndex, size,
            Sort.by(Sort.Direction.DESC, "lastMessageAt", "updatedAt", "id"));

        User currentUser = currentUserService.getCurrentUser();
        Set<String> roles = resolveRoles(currentUser);
        Page<Consultation> pageResult;
        if (roles.contains(ROLE_ADMIN) || roles.contains(ROLE_DEPT)) {
            pageResult = consultationRepository.findAll(pageable);
        } else {
            pageResult = consultationRepository.findByCreatedBy(currentUser, pageable);
        }

        List<ConsultationSummary> items = pageResult.getContent().stream()
            .map(consultation -> toSummary(consultation, currentUser))
            .collect(Collectors.toList());

        return new ConsultationPageResponse(items, pageResult.getTotalElements(), pageIndex + 1, size);
    }

    @Override
    public ConsultationSummary createConsultation(ConsultationCreateRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Set<String> roles = resolveRoles(currentUser);
        if (!roles.contains(ROLE_FARMER) && !roles.contains(ROLE_ADMIN)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账号无权发起咨询");
        }

        Consultation consultation = new Consultation();
        consultation.setSubject(request.subject());
        consultation.setCropType(request.cropType());
        consultation.setDescription(request.description());
        consultation.setPriority(defaultPriority(request.priority()));
        consultation.setStatus("pending");
        consultation.setCreatedBy(currentUser);
        Consultation saved = consultationRepository.save(consultation);

        ConsultationParticipant participant = new ConsultationParticipant();
        participant.setConsultation(saved);
        participant.setUser(currentUser);
        participant.setRoleCode(resolvePrimaryRole(currentUser));
        participant.setOwner(true);
        participant.setLastReadAt(LocalDateTime.now());
        participantRepository.save(participant);
        attachParticipant(saved, participant);

        if (StringUtils.hasText(request.description())) {
            ConsultationMessage message = new ConsultationMessage();
            message.setConsultation(saved);
            message.setSender(currentUser);
            message.setSenderRole(resolvePrimaryRole(currentUser));
            message.setContent(request.description());
            ConsultationMessage savedMessage = messageRepository.save(message);
            saved.setLastMessageAt(savedMessage.getCreatedAt());
            consultationRepository.save(saved);
        }

        return toSummary(saved, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationMessagesResponse getMessages(Long consultationId, int pageSize) {
        Consultation consultation = loadAccessibleConsultation(consultationId);
        User currentUser = currentUserService.getCurrentUser();
        List<ConsultationMessage> messages = messageRepository.findByConsultationIdOrderByCreatedAtAsc(consultationId);
        int limit = pageSize <= 0 ? messages.size() : Math.min(pageSize, messages.size());
        List<ConsultationMessageResponse> items = messages.stream()
            .sorted(Comparator.comparing(ConsultationMessage::getCreatedAt))
            .limit(limit)
            .map(this::toMessageResponse)
            .collect(Collectors.toList());
        return new ConsultationMessagesResponse(toSummary(consultation, currentUser), items, messages.size());
    }

    @Override
    public ConsultationMessageResponse sendMessage(Long consultationId, ConsultationMessageRequest request) {
        return sendMessage(consultationId, request.content(), List.of());
    }

    @Override
    public ConsultationMessageResponse sendMessage(Long consultationId, String content, List<MultipartFile> attachments) {
        Consultation consultation = loadAccessibleConsultation(consultationId);
        User currentUser = currentUserService.getCurrentUser();
        String trimmedContent = content == null ? null : content.trim();
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        if (!StringUtils.hasText(trimmedContent) && !hasAttachments) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请输入消息内容或上传附件");
        }

        ConsultationMessage message = new ConsultationMessage();
        message.setConsultation(consultation);
        message.setSender(currentUser);
        message.setSenderRole(resolvePrimaryRole(currentUser));
        message.setContent(trimmedContent);
        ConsultationMessage savedMessage = messageRepository.save(message);

        if (hasAttachments) {
            ensureAttachmentDirectory();
            List<ConsultationAttachment> savedAttachments = new ArrayList<>();
            for (MultipartFile file : attachments) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                ConsultationAttachment attachment = storeAttachment(consultation.getId(), savedMessage, file);
                savedAttachments.add(attachment);
            }
            if (!savedAttachments.isEmpty()) {
                savedMessage.setAttachments(savedAttachments);
            }
        }

        consultation.setLastMessageAt(savedMessage.getCreatedAt());
        consultationRepository.save(consultation);

        ConsultationParticipant participant = participantRepository
            .findByConsultationIdAndUserId(consultationId, currentUser.getId())
            .orElseGet(() -> {
                ConsultationParticipant created = new ConsultationParticipant();
                created.setConsultation(consultation);
                created.setUser(currentUser);
                created.setRoleCode(resolvePrimaryRole(currentUser));
                created.setOwner(false);
                attachParticipant(consultation, created);
                return created;
            });
        participant.setLastReadAt(LocalDateTime.now());
        participantRepository.save(participant);

        return toMessageResponse(savedMessage);
    }

    @Override
    public ConsultationSummary updateConsultation(Long consultationId, ConsultationUpdateRequest request) {
        Consultation consultation = loadAccessibleConsultation(consultationId);
        User currentUser = currentUserService.getCurrentUser();
        Set<String> roles = resolveRoles(currentUser);
        if (!roles.contains(ROLE_ADMIN) && !roles.contains(ROLE_DEPT) && !consultation.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "没有权限更新该咨询");
        }

        if (request.status() != null && !request.status().isBlank()) {
            String normalizedStatus = request.status().trim();
            consultation.setStatus(normalizedStatus);
            if ("resolved".equalsIgnoreCase(normalizedStatus) || "closed".equalsIgnoreCase(normalizedStatus)) {
                consultation.setClosedAt(LocalDateTime.now());
            } else {
                consultation.setClosedAt(null);
            }
        }
        if (request.priority() != null && !request.priority().isBlank()) {
            consultation.setPriority(request.priority().trim());
        }
        if (request.assignedTo() != null) {
            User assignee = userRepository.findById(request.assignedTo())
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "指定的处理人不存在"));
            consultation.setAssignedTo(assignee);
        }

        Consultation updated = consultationRepository.save(consultation);
        return toSummary(updated, currentUser);
    }

    @Override
    public void markAsRead(Long consultationId) {
        Consultation consultation = loadAccessibleConsultation(consultationId);
        User currentUser = currentUserService.getCurrentUser();
        ConsultationParticipant participant = participantRepository
            .findByConsultationIdAndUserId(consultationId, currentUser.getId())
            .orElseGet(() -> {
                ConsultationParticipant created = new ConsultationParticipant();
                created.setConsultation(consultation);
                created.setUser(currentUser);
                created.setRoleCode(resolvePrimaryRole(currentUser));
                created.setOwner(false);
                attachParticipant(consultation, created);
                return created;
            });
        participant.setLastReadAt(LocalDateTime.now());
        participantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationAttachmentDownload loadAttachment(Long consultationId, Long attachmentId) {
        loadAccessibleConsultation(consultationId);
        ConsultationAttachment attachment = attachmentRepository
            .findByIdAndMessageConsultationId(attachmentId, consultationId)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "附件不存在"));
        Path path = Path.of(attachment.getStoragePath());
        if (!Files.exists(path)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "附件文件不存在");
        }
        Resource resource = new FileSystemResource(path);
        return new ConsultationAttachmentDownload(resource,
            attachment.getOriginalName() != null ? attachment.getOriginalName() : attachment.getFileName(),
            attachment.getContentType());
    }

    private ConsultationAttachment storeAttachment(Long consultationId, ConsultationMessage message, MultipartFile file) {
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : file.getName();
        String extension = StringUtils.getFilenameExtension(originalName);
        String generatedName = UUID.randomUUID().toString().replaceAll("-", "");
        String fileName = extension == null ? generatedName : generatedName + "." + extension;
        Path directory = attachmentRoot.resolve(String.valueOf(consultationId));
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            ConsultationAttachment attachment = new ConsultationAttachment();
            attachment.setMessage(message);
            attachment.setFileName(fileName);
            attachment.setOriginalName(originalName);
            attachment.setContentType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setStoragePath(target.toAbsolutePath().toString());
            return attachmentRepository.save(attachment);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "保存附件失败: " + ex.getMessage());
        }
    }

    private void ensureAttachmentDirectory() {
        try {
            Files.createDirectories(attachmentRoot);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "无法创建附件目录: " + ex.getMessage());
        }
    }

    private ConsultationSummary toSummary(Consultation consultation, User currentUser) {
        ConsultationMessage lastMessage = messageRepository
            .findFirstByConsultationIdOrderByCreatedAtDesc(consultation.getId())
            .orElse(null);
        ConsultationMessageResponse lastMessageResponse = lastMessage == null ? null : toMessageResponse(lastMessage);
        long unreadCount = calculateUnreadCount(consultation.getId(), currentUser.getId());
        List<ConsultationParticipantResponse> participants = consultation.getParticipants().stream()
            .map(participant -> new ConsultationParticipantResponse(
                participant.getUser().getId(),
                participant.getUser().getFullName() != null ? participant.getUser().getFullName() : participant.getUser().getUsername(),
                participant.getRoleCode(),
                participant.isOwner(),
                participant.getUser().getId().equals(currentUser.getId())
            ))
            .collect(Collectors.toList());
        return new ConsultationSummary(
            consultation.getId(),
            consultation.getSubject(),
            consultation.getCropType(),
            consultation.getStatus(),
            consultation.getPriority(),
            consultation.getCreatedAt(),
            consultation.getUpdatedAt(),
            lastMessageResponse,
            unreadCount,
            participants
        );
    }

    private ConsultationMessageResponse toMessageResponse(ConsultationMessage message) {
        List<ConsultationAttachmentResponse> attachments = message.getAttachments().stream()
            .sorted(Comparator.comparing(ConsultationAttachment::getId))
            .map(attachment -> new ConsultationAttachmentResponse(
                attachment.getId(),
                attachment.getOriginalName() != null ? attachment.getOriginalName() : attachment.getFileName(),
                String.format("/api/consultations/%d/attachments/%d", message.getConsultation().getId(), attachment.getId()),
                attachment.getContentType()
            ))
            .collect(Collectors.toList());
        User sender = message.getSender();
        String senderName = sender.getFullName() != null && !sender.getFullName().isBlank()
            ? sender.getFullName()
            : sender.getUsername();
        return new ConsultationMessageResponse(
            message.getId(),
            message.getConsultation().getId(),
            sender.getId(),
            senderName,
            message.getSenderRole(),
            message.getContent(),
            message.getCreatedAt(),
            attachments
        );
    }

    private void attachParticipant(Consultation consultation, ConsultationParticipant participant) {
        if (consultation.getParticipants() == null) {
            consultation.setParticipants(new ArrayList<>());
        }
        boolean exists = consultation.getParticipants().stream()
            .anyMatch(existing -> existing.getUser().getId().equals(participant.getUser().getId()));
        if (!exists) {
            consultation.getParticipants().add(participant);
        }
    }

    private Consultation loadAccessibleConsultation(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "咨询不存在"));
        User currentUser = currentUserService.getCurrentUser();
        Set<String> roles = resolveRoles(currentUser);
        if (!consultation.getCreatedBy().getId().equals(currentUser.getId())
            && !roles.contains(ROLE_ADMIN)
            && !roles.contains(ROLE_DEPT)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "没有访问该咨询的权限");
        }
        return consultation;
    }

    private long calculateUnreadCount(Long consultationId, Long currentUserId) {
        return participantRepository.findByConsultationIdAndUserId(consultationId, currentUserId)
            .map(participant -> participant.getLastReadAt() == null
                ? messageRepository.countByConsultationIdAndSenderIdNot(consultationId, currentUserId)
                : messageRepository.countByConsultationIdAndCreatedAtAfterAndSenderIdNot(consultationId, participant.getLastReadAt(), currentUserId))
            .orElseGet(() -> messageRepository.countByConsultationIdAndSenderIdNot(consultationId, currentUserId));
    }

    private Set<String> resolveRoles(User user) {
        return user.getRoles().stream()
            .map(Role::getCode)
            .filter(code -> code != null && !code.isBlank())
            .collect(Collectors.toSet());
    }

    private String resolvePrimaryRole(User user) {
        Set<String> roles = resolveRoles(user);
        if (roles.contains(ROLE_FARMER)) {
            return ROLE_FARMER;
        }
        if (roles.contains(ROLE_DEPT)) {
            return ROLE_DEPT;
        }
        if (roles.contains(ROLE_ADMIN)) {
            return ROLE_ADMIN;
        }
        return roles.stream().findFirst().orElse("USER");
    }

    private String defaultPriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return "normal";
        }
        return priority.trim();
    }
}
