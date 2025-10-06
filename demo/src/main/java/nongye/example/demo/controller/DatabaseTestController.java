package nongye.example.demo.controller;

import nongye.example.demo.entity.User;
import nongye.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class DatabaseTestController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/test-db")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试数据库连接
            long userCount = userRepository.count();
            result.put("userCount", userCount);
            result.put("databaseConnected", true);
            
            // 查找admin用户
            Optional<User> adminUser = userRepository.findByUsername("admin");
            if (adminUser.isPresent()) {
                User user = adminUser.get();
                result.put("adminUserFound", true);
                result.put("adminPassword", user.getPassword());
                result.put("adminStatus", user.getStatus());
            } else {
                result.put("adminUserFound", false);
            }
            
        } catch (Exception e) {
            result.put("databaseConnected", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}

