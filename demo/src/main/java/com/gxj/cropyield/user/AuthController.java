package com.gxj.cropyield.user;

import com.gxj.cropyield.user.dto.LoginRequest;
import com.gxj.cropyield.user.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;

    public AuthController(AuthenticationManager authenticationManager, UserAccountRepository userAccountRepository) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<UserProfileResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.username(), request.password());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        UserAccount account = userAccountRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow();
        account.setLastLoginAt(LocalDateTime.now());
        userAccountRepository.save(account);

        return ResponseEntity.ok(toResponse(account));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userAccountRepository.findByUsernameIgnoreCase(userDetails.getUsername())
                    .map(account -> ResponseEntity.ok(toResponse(account)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private UserProfileResponse toResponse(UserAccount account) {
        Set<String> roles = account.getRoles()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        return new UserProfileResponse(
                account.getUsername(),
                account.getDisplayName(),
                account.getEmail(),
                account.getPhone(),
                account.getOrganization(),
                roles,
                account.getLastLoginAt()
        );
    }
}
