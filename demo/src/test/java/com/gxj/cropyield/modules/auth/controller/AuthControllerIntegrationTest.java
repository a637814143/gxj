package com.gxj.cropyield.modules.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxj.cropyield.modules.auth.dto.RefreshTokenRequest;
import com.gxj.cropyield.modules.auth.dto.RegisterRequest;
import com.gxj.cropyield.modules.auth.entity.LoginLog;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.LoginLogRepository;
import com.gxj.cropyield.modules.auth.repository.RefreshTokenRepository;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private LoginLogRepository loginLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final Set<String> createdUsernames = new HashSet<>();

    @AfterEach
    void tearDown() {
        if (createdUsernames.isEmpty()) {
            return;
        }

        List<LoginLog> loginLogs = loginLogRepository.findAll().stream()
            .filter(log -> log.getUsername() != null && createdUsernames.contains(log.getUsername()))
            .collect(Collectors.toList());
        loginLogRepository.deleteAll(loginLogs);

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            createdUsernames.stream()
                .map(userRepository::findByUsername)
                .flatMap(Optional::stream)
                .forEach(user -> {
                    refreshTokenRepository.deleteByUser(user);
                    userRepository.delete(user);
                });
        });

        createdUsernames.clear();
    }

    @Test
    void registerUserPersistsAccountAndAllowsAccessForAgricultureDepartmentRole() throws Exception {
        String username = "agri_user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        RegisterRequest request = new RegisterRequest(
            username,
            "Password123!",
            "农业部门测试用户",
            "agri-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
            "AGRICULTURE_DEPT"
        );

        MvcResult mvcResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.tokens.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.tokens.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.data.user.roles").isArray())
            .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(responseContent).path("data");
        String accessToken = root.path("tokens").path("accessToken").asText();
        String refreshToken = root.path("tokens").path("refreshToken").asText();

        User savedUser = userRepository.findWithRolesByUsername(username).orElseThrow();
        createdUsernames.add(savedUser.getUsername());

        assertThat(passwordEncoder.matches("Password123!", savedUser.getPassword())).isTrue();
        assertThat(savedUser.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
            .contains("AGRICULTURE_DEPT");

        assertThat(refreshTokenRepository.findByToken(refreshToken)).isPresent();

        assertThat(loginLogRepository.findAll().stream()
            .filter(log -> username.equals(log.getUsername()))
            .anyMatch(LoginLog::isSuccess)).isTrue();

        mockMvc.perform(get("/api/base/crops")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

}
