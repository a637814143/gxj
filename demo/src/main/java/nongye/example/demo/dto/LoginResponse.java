package nongye.example.demo.dto;

import lombok.Data;
import nongye.example.demo.entity.User;

import java.util.Set;

@Data
public class LoginResponse {
    
    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private String realName;
    private String organization;
    private String regionCode;
    private Set<String> roles;
    private Long expiresIn;
    
    public LoginResponse(String token, User user, Long expiresIn) {
        this.token = token;
        this.userId = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.realName = user.getRealName();
        this.organization = user.getOrganization();
        this.regionCode = user.getRegionCode();
        this.roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(java.util.stream.Collectors.toSet());
        this.expiresIn = expiresIn;
    }
}
