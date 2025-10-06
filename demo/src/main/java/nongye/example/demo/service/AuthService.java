package nongye.example.demo.service;

import nongye.example.demo.dto.LoginRequest;
import nongye.example.demo.dto.LoginResponse;
import nongye.example.demo.entity.User;
import nongye.example.demo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserService userService;
    
    @Value("${jwt.expiration}")
    private Long jwtExpiration;
    
    private Set<String> blacklistedTokens = new HashSet<>();
    
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        User user = (User) authentication.getPrincipal();
        String token = jwtUtil.generateToken(user);
        
        return new LoginResponse(token, user, jwtExpiration);
    }
    
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            blacklistedTokens.add(jwt);
        }
    }
    
    public LoginResponse refreshToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            
            if (blacklistedTokens.contains(jwt)) {
                throw new RuntimeException("Token已被注销");
            }
            
            String username = jwtUtil.getUsernameFromToken(jwt);
            User user = (User) userService.loadUserByUsername(username);
            
            if (jwtUtil.validateToken(jwt, user)) {
                String newToken = jwtUtil.generateToken(user);
                return new LoginResponse(newToken, user, jwtExpiration);
            }
        }
        
        throw new RuntimeException("Token无效");
    }
    
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
