package com.gxj.cropyield.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.common.security.CustomUserDetailsService;
import com.gxj.cropyield.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/login", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/datasets/**", "/api/dashboard/**", "/api/base/**", "/api/forecast/models/**", "/api/forecast/tasks/**", "/api/report/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT", "FARMER")
                .requestMatchers(HttpMethod.POST, "/api/forecast/models/**", "/api/forecast/tasks/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT")
                .requestMatchers(HttpMethod.GET, "/api/report/export/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT")
                .requestMatchers(HttpMethod.POST, "/api/report/**", "/api/report/export/**").hasAnyRole("ADMIN", "AGRICULTURE_DEPT")
                .requestMatchers(HttpMethod.POST, "/api/datasets/**", "/api/base/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/datasets/**", "/api/base/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/datasets/**", "/api/base/**").hasRole("ADMIN")
                .requestMatchers("/api/system/logs/**", "/api/auth/users/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> writeErrorResponse(response, ResultCode.UNAUTHORIZED, "未认证的访问"))
                .accessDeniedHandler((request, response, accessDeniedException) -> writeErrorResponse(response, ResultCode.FORBIDDEN, "没有访问权限"))
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeErrorResponse(jakarta.servlet.http.HttpServletResponse response, ResultCode code, String message) throws java.io.IOException {
        response.setStatus(code.getCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.failure(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(java.util.List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        configuration.addAllowedHeader(CorsConfiguration.ALL);
        configuration.addAllowedMethod(CorsConfiguration.ALL);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
