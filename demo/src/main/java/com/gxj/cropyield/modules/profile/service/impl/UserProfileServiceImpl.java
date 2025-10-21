package com.gxj.cropyield.modules.profile.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.entity.LoginLog;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.entity.UserProfile;
import com.gxj.cropyield.modules.auth.repository.LoginLogRepository;
import com.gxj.cropyield.modules.auth.repository.UserProfileRepository;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import com.gxj.cropyield.modules.profile.dto.BusinessSection;
import com.gxj.cropyield.modules.profile.dto.CustomField;
import com.gxj.cropyield.modules.profile.dto.DataCleanupRecord;
import com.gxj.cropyield.modules.profile.dto.DataOperations;
import com.gxj.cropyield.modules.profile.dto.DeviceInfo;
import com.gxj.cropyield.modules.profile.dto.ExportForm;
import com.gxj.cropyield.modules.profile.dto.NotificationPreferences;
import com.gxj.cropyield.modules.profile.dto.PersonalizationSettings;
import com.gxj.cropyield.modules.profile.dto.SecuritySettings;
import com.gxj.cropyield.modules.profile.dto.UpdatePasswordRequest;
import com.gxj.cropyield.modules.profile.dto.UpdateUserProfileRequest;
import com.gxj.cropyield.modules.profile.dto.UserProfileResponse;
import com.gxj.cropyield.modules.profile.service.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final LoginLogRepository loginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public UserProfileServiceImpl(UserRepository userRepository,
                                  UserProfileRepository userProfileRepository,
                                  LoginLogRepository loginLogRepository,
                                  PasswordEncoder passwordEncoder,
                                  ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.loginLogRepository = loginLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        User user = resolveCurrentUser();
        UserProfile profile = findOrCreateProfile(user);
        return buildResponse(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        User user = resolveCurrentUser();
        UserProfile profile = findOrCreateProfile(user);

        StoredSecuritySettings securitySettings = normalizeSecurity(request == null ? null : request.security());
        BusinessSection business = normalizeBusiness(request == null ? null : request.business());
        PersonalizationSettings personalization = normalizePersonalization(request == null ? null : request.personalization());
        DataOperations dataOperations = normalizeDataOperations(request == null ? null : request.dataOperations());

        profile.setSecuritySettings(writeJson(securitySettings));
        profile.setBusinessInfo(writeJson(business));
        profile.setPersonalizationSettings(writeJson(personalization));
        profile.setDataOperations(writeJson(dataOperations));

        UserProfile saved = userProfileRepository.save(profile);
        return buildResponse(user, saved);
    }

    @Override
    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求参数不能为空");
        }
        User user = resolveCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        UserProfile profile = findOrCreateProfile(user);
        StoredSecuritySettings storedSecurity = readSecurity(profile.getSecuritySettings());
        StoredSecuritySettings updatedSecurity = new StoredSecuritySettings(
            LocalDateTime.now(),
            storedSecurity.devices()
        );
        profile.setSecuritySettings(writeJson(updatedSecurity));
        userProfileRepository.save(profile);
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在"));
    }

    private UserProfile findOrCreateProfile(User user) {
        Optional<UserProfile> existing = userProfileRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setSecuritySettings(writeJson(defaultSecuritySettings()));
        profile.setBusinessInfo(writeJson(defaultBusinessSection()));
        profile.setPersonalizationSettings(writeJson(defaultPersonalization()));
        profile.setDataOperations(writeJson(defaultDataOperations()));
        return userProfileRepository.save(profile);
    }

    private UserProfileResponse buildResponse(User user, UserProfile profile) {
        StoredSecuritySettings storedSecurity = readSecurity(profile.getSecuritySettings());
        BusinessSection business = readBusiness(profile.getBusinessInfo());
        PersonalizationSettings personalization = readPersonalization(profile.getPersonalizationSettings());
        DataOperations dataOperations = readDataOperations(profile.getDataOperations());

        LocalDateTime lastLoginAt = loginLogRepository
            .findFirstByUsernameAndSuccessOrderByCreatedAtDesc(user.getUsername(), true)
            .map(LoginLog::getCreatedAt)
            .orElse(null);

        SecuritySettings security = new SecuritySettings(
            lastLoginAt,
            storedSecurity.lastPasswordChange(),
            storedSecurity.devices()
        );
        return new UserProfileResponse(security, business, personalization, dataOperations);
    }

    private StoredSecuritySettings normalizeSecurity(SecuritySettings security) {
        if (security == null) {
            return defaultSecuritySettings();
        }
        List<DeviceInfo> devices = normalizeDevices(security.devices());
        return new StoredSecuritySettings(security.lastPasswordChange(), devices);
    }

    private BusinessSection normalizeBusiness(BusinessSection business) {
        if (business == null) {
            return defaultBusinessSection();
        }
        Map<String, Object> form = business.form() == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(business.form());
        List<CustomField> customFields = normalizeCustomFields(business.customFields());
        return new BusinessSection(form, customFields);
    }

    private PersonalizationSettings normalizePersonalization(PersonalizationSettings personalization) {
        if (personalization == null) {
            return defaultPersonalization();
        }
        NotificationPreferences notifications = personalization.notifications();
        NotificationPreferences normalizedNotifications = notifications == null
            ? defaultNotificationPreferences()
            : new NotificationPreferences(
                booleanOrDefault(notifications.email(), Boolean.TRUE),
                booleanOrDefault(notifications.sms(), Boolean.FALSE),
                booleanOrDefault(notifications.inApp(), Boolean.TRUE),
                booleanOrDefault(notifications.security(), Boolean.TRUE)
            );
        return new PersonalizationSettings(
            safeTrim(personalization.theme()),
            safeTrim(personalization.accentColor()),
            safeTrim(personalization.density()),
            safeTrim(personalization.digestFrequency()),
            booleanOrDefault(personalization.quietMode(), Boolean.TRUE),
            normalizedNotifications
        );
    }

    private DataOperations normalizeDataOperations(DataOperations dataOperations) {
        if (dataOperations == null) {
            return defaultDataOperations();
        }
        List<String> lastSelection = normalizeStringList(dataOperations.lastSelection());
        List<DataCleanupRecord> history = normalizeCleanupHistory(dataOperations.history());
        ExportForm exportForm = normalizeExportForm(dataOperations.exportForm());
        return new DataOperations(lastSelection, history, safeTrim(dataOperations.lastExportTime()), exportForm);
    }

    private List<DeviceInfo> normalizeDevices(List<DeviceInfo> devices) {
        if (devices == null || devices.isEmpty()) {
            return List.of();
        }
        return devices.stream()
            .map(device -> new DeviceInfo(
                device.id(),
                safeTrim(device.name()),
                safeTrim(device.browser()),
                safeTrim(device.system()),
                safeTrim(device.location()),
                device.lastActive(),
                booleanOrDefault(device.trusted(), Boolean.TRUE)
            ))
            .collect(Collectors.toList());
    }

    private List<CustomField> normalizeCustomFields(List<CustomField> customFields) {
        if (customFields == null || customFields.isEmpty()) {
            return List.of();
        }
        return customFields.stream()
            .map(field -> new CustomField(field.id(), safeTrim(field.label()), safeTrim(field.value())))
            .collect(Collectors.toList());
    }

    private List<DataCleanupRecord> normalizeCleanupHistory(List<DataCleanupRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
            .map(record -> new DataCleanupRecord(
                record.id(),
                safeTrim(record.time()),
                safeTrim(record.description()),
                defaultIfBlank(record.type(), "primary")
            ))
            .collect(Collectors.toList());
    }

    private ExportForm normalizeExportForm(ExportForm exportForm) {
        if (exportForm == null) {
            return defaultExportForm();
        }
        return new ExportForm(
            safeTrim(exportForm.scope()),
            safeTrim(exportForm.format()),
            normalizeStringList(exportForm.range()),
            booleanOrDefault(exportForm.includeAudit(), Boolean.TRUE)
        );
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .map(this::safeTrim)
            .filter(value -> value != null && !value.isEmpty())
            .collect(Collectors.toList());
    }

    private StoredSecuritySettings readSecurity(String json) {
        StoredSecuritySettings stored = readJson(json, StoredSecuritySettings.class, this::defaultSecuritySettings);
        List<DeviceInfo> devices = normalizeDevices(stored.devices());
        return new StoredSecuritySettings(stored.lastPasswordChange(), devices);
    }

    private BusinessSection readBusiness(String json) {
        BusinessSection stored = readJson(json, BusinessSection.class, this::defaultBusinessSection);
        return normalizeBusiness(stored);
    }

    private PersonalizationSettings readPersonalization(String json) {
        PersonalizationSettings stored = readJson(json, PersonalizationSettings.class, this::defaultPersonalization);
        return normalizePersonalization(stored);
    }

    private DataOperations readDataOperations(String json) {
        DataOperations stored = readJson(json, DataOperations.class, this::defaultDataOperations);
        return normalizeDataOperations(stored);
    }

    private StoredSecuritySettings defaultSecuritySettings() {
        return new StoredSecuritySettings(null, List.of());
    }

    private BusinessSection defaultBusinessSection() {
        return new BusinessSection(new LinkedHashMap<>(), List.of());
    }

    private NotificationPreferences defaultNotificationPreferences() {
        return new NotificationPreferences(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE);
    }

    private PersonalizationSettings defaultPersonalization() {
        return new PersonalizationSettings(
            "system",
            "#2563eb",
            "comfortable",
            "daily",
            Boolean.TRUE,
            defaultNotificationPreferences()
        );
    }

    private ExportForm defaultExportForm() {
        return new ExportForm("profile", "xlsx", List.of(), Boolean.TRUE);
    }

    private DataOperations defaultDataOperations() {
        return new DataOperations(List.of(), List.of(), null, defaultExportForm());
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "保存个人资料失败");
        }
    }

    private <T> T readJson(String json, Class<T> type, java.util.function.Supplier<T> fallback) {
        if (!StringUtils.hasText(json)) {
            return fallback.get();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return fallback.get();
        }
    }

    private Boolean booleanOrDefault(Boolean value, Boolean fallback) {
        return value == null ? fallback : value;
    }

    private String safeTrim(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        String trimmed = safeTrim(value);
        return trimmed == null ? fallback : trimmed;
    }

    private record StoredSecuritySettings(
        LocalDateTime lastPasswordChange,
        List<DeviceInfo> devices
    ) {
    }
}
