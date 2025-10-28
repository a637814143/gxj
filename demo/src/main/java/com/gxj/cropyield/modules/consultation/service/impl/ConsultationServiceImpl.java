package com.gxj.cropyield.modules.consultation.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.auth.service.CurrentUserService;
import com.gxj.cropyield.modules.consultation.dto.ConsultationCreateRequest;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessageRequest;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessageResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessagesResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationPageResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationParticipantResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationSummary;
import com.gxj.cropyield.modules.consultation.dto.ConsultationUpdateRequest;
import com.gxj.cropyield.modules.consultation.entity.Consultation;
import com.gxj.cropyield.modules.consultation.dto.ConsultationDepartmentOption;
import com.gxj.cropyield.modules.consultation.entity.ConsultationMessage;
import com.gxj.cropyield.modules.consultation.entity.ConsultationParticipant;
import com.gxj.cropyield.modules.consultation.repository.ConsultationMessageRepository;
import com.gxj.cropyield.modules.consultation.repository.ConsultationParticipantRepository;
import com.gxj.cropyield.modules.consultation.repository.ConsultationRepository;
import com.gxj.cropyield.modules.consultation.service.ConsultationDepartmentDirectory;
import com.gxj.cropyield.modules.consultation.service.ConsultationService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final ConsultationDepartmentDirectory departmentDirectory;

    public ConsultationServiceImpl(ConsultationRepository consultationRepository,
                                    ConsultationMessageRepository messageRepository,
                                    ConsultationParticipantRepository participantRepository,
                                    CurrentUserService currentUserService,
                                    UserRepository userRepository,
                                    ConsultationDepartmentDirectory departmentDirectory) {
        this.consultationRepository = consultationRepository;
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.departmentDirectory = departmentDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationPageResponse listConsultations(int page, int pageSize, String status, String keyword) {
        int pageIndex = Math.max(page - 1, 0);
        int size = pageSize <= 0 ? 20 : pageSize;
        Pageable pageable = PageRequest.of(pageIndex, size,
            Sort.by(Sort.Direction.DESC, "lastMessageAt", "updatedAt", "id"));

        User currentUser = currentUserService.getCurrentUser();
        Set<String> roles = resolveRoles(currentUser);
        final boolean isAdmin = roles.contains(ROLE_ADMIN);
        final boolean isDepartmentUser = roles.contains(ROLE_DEPT);
        final String normalizedDepartmentCode = StringUtils.hasText(currentUser.getDepartmentCode())
            ? currentUser.getDepartmentCode().trim().toLowerCase()
            : null;

        if (isDepartmentUser && !isAdmin && normalizedDepartmentCode == null) {
            return new ConsultationPageResponse(Collections.emptyList(), 0, pageIndex + 1, size);
        }
        final String statusFilter = StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)
            ? status.trim().toLowerCase()
            : null;
        final String keywordFilter = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;

        Page<Consultation> pageResult = consultationRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isAdmin) {
                // 管理员可以查看全部会话
            } else if (isDepartmentUser) {
                predicates.add(cb.equal(cb.lower(root.get("departmentCode")), normalizedDepartmentCode));
            } else {
                predicates.add(cb.equal(root.get("createdBy"), currentUser));
            }
            if (statusFilter != null) {
                predicates.add(cb.equal(cb.lower(root.get("status")), statusFilter));
            }
            if (keywordFilter != null) {
                String pattern = "%" + keywordFilter + "%";
                Predicate subjectPredicate = cb.like(cb.lower(root.get("subject")), pattern);
                Predicate cropPredicate = cb.like(cb.lower(root.get("cropType")), pattern);
                Predicate descriptionPredicate = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(subjectPredicate, cropPredicate, descriptionPredicate));
            }
            if (predicates.isEmpty()) {
                return null;
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

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

        ConsultationDepartmentOption department = departmentDirectory.findByCode(request.departmentCode())
            .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "请选择有效的咨询部门"));

        User departmentAssignee = resolveDepartmentAssignee(department.code());

        Consultation consultation = new Consultation();
        consultation.setSubject(request.subject());
        consultation.setCropType(request.cropType());
        consultation.setDescription(request.description());
        consultation.setPriority(defaultPriority(request.priority()));
        consultation.setStatus("pending");
        consultation.setCreatedBy(currentUser);
        consultation.setDepartmentCode(department.code());
        if (departmentAssignee != null) {
            consultation.setAssignedTo(departmentAssignee);
        }
        Consultation saved = consultationRepository.save(consultation);

        ConsultationParticipant participant = new ConsultationParticipant();
        participant.setConsultation(saved);
        participant.setUser(currentUser);
        participant.setRoleCode(resolvePrimaryRole(currentUser));
        participant.setOwner(true);
        participant.setLastReadAt(LocalDateTime.now());
        participantRepository.save(participant);
        attachParticipant(saved, participant);

        if (departmentAssignee != null) {
            ConsultationParticipant deptParticipant = new ConsultationParticipant();
            deptParticipant.setConsultation(saved);
            deptParticipant.setUser(departmentAssignee);
            deptParticipant.setRoleCode(ROLE_DEPT);
            deptParticipant.setOwner(false);
            participantRepository.save(deptParticipant);
            attachParticipant(saved, deptParticipant);
        }

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
        Consultation consultation = loadAccessibleConsultation(consultationId);
        if ("closed".equalsIgnoreCase(consultation.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该对话已结束，无法继续发送消息");
        }
        User currentUser = currentUserService.getCurrentUser();
        String trimmedContent = request == null || request.content() == null ? null : request.content().trim();
        if (!StringUtils.hasText(trimmedContent)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能为空");
        }

        ConsultationMessage message = new ConsultationMessage();
        message.setConsultation(consultation);
        message.setSender(currentUser);
        message.setSenderRole(resolvePrimaryRole(currentUser));
        message.setContent(trimmedContent);
        ConsultationMessage savedMessage = messageRepository.save(message);

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
    public ConsultationSummary closeConsultation(Long consultationId) {
        Consultation consultation = loadAccessibleConsultation(consultationId);
        User currentUser = currentUserService.getCurrentUser();
        if ("closed".equalsIgnoreCase(consultation.getStatus())) {
            return toSummary(consultation, currentUser);
        }
        consultation.setStatus("closed");
        consultation.setClosedAt(LocalDateTime.now());
        Consultation updated = consultationRepository.save(consultation);
        return toSummary(updated, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationDepartmentOption> listDepartments() {
        return departmentDirectory.listActiveDepartments();
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
        User owner = consultation.getCreatedBy();
        String ownerName = owner == null
            ? null
            : (StringUtils.hasText(owner.getFullName()) ? owner.getFullName() : owner.getUsername());
        User assignee = consultation.getAssignedTo();
        String assigneeName = assignee == null
            ? null
            : (StringUtils.hasText(assignee.getFullName()) ? assignee.getFullName() : assignee.getUsername());
        long messageCount = messageRepository.countByConsultationId(consultation.getId());
        String departmentName = departmentDirectory.findByCode(consultation.getDepartmentCode())
            .map(ConsultationDepartmentOption::name)
            .orElse(null);
        return new ConsultationSummary(
            consultation.getId(),
            consultation.getSubject(),
            consultation.getCropType(),
            consultation.getStatus(),
            consultation.getPriority(),
            consultation.getDepartmentCode(),
            departmentName,
            consultation.getCreatedAt(),
            consultation.getUpdatedAt(),
            consultation.getClosedAt(),
            lastMessageResponse,
            unreadCount,
            participants,
            consultation.getDescription(),
            owner != null ? owner.getId() : null,
            ownerName,
            assignee != null ? assignee.getId() : null,
            assigneeName,
            messageCount
        );
    }

    private ConsultationMessageResponse toMessageResponse(ConsultationMessage message) {
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
            message.getCreatedAt()
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
        boolean isOwner = consultation.getCreatedBy().getId().equals(currentUser.getId());
        if (isOwner) {
            return consultation;
        }
        if (roles.contains(ROLE_ADMIN)) {
            return consultation;
        }
        if (roles.contains(ROLE_DEPT)) {
            String userDepartment = currentUser.getDepartmentCode();
            String consultationDepartment = consultation.getDepartmentCode();
            if (StringUtils.hasText(userDepartment) && StringUtils.hasText(consultationDepartment)
                && userDepartment.trim().equalsIgnoreCase(consultationDepartment.trim())) {
                return consultation;
            }
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "没有访问该咨询的权限");
    }

    private long calculateUnreadCount(Long consultationId, Long currentUserId) {
        return participantRepository.findByConsultationIdAndUserId(consultationId, currentUserId)
            .map(participant -> participant.getLastReadAt() == null
                ? messageRepository.countByConsultationIdAndSenderIdNot(consultationId, currentUserId)
                : messageRepository.countByConsultationIdAndCreatedAtAfterAndSenderIdNot(consultationId, participant.getLastReadAt(), currentUserId))
            .orElseGet(() -> messageRepository.countByConsultationIdAndSenderIdNot(consultationId, currentUserId));
    }

    private User resolveDepartmentAssignee(String departmentCode) {
        if (!StringUtils.hasText(departmentCode)) {
            return null;
        }
        return userRepository.findByRoleAndDepartment(ROLE_DEPT, departmentCode.trim())
            .stream()
            .findFirst()
            .orElse(null);
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
