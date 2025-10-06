package nongye.example.demo.controller;

import nongye.example.demo.entity.User;
import nongye.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class TestController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/test-user/{username}")
    public Map<String, Object> testUser(@PathVariable String username) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                response.put("found", true);
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("status", user.getStatus());
                response.put("password", user.getPassword());
                response.put("roles", user.getRoles());
            } else {
                response.put("found", false);
                response.put("message", "用户不存在");
            }
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/test-all-users")
    public Map<String, Object> testAllUsers() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("users", userRepository.findAll());
            response.put("count", userRepository.count());
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
}

