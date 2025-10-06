package nongye.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PasswordTestController {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/test-password/{password}")
    public ResponseEntity<Map<String, Object>> testPassword(@PathVariable String password) {
        Map<String, Object> result = new HashMap<>();
        
        // 生成加密密码
        String encodedPassword = passwordEncoder.encode(password);
        result.put("originalPassword", password);
        result.put("encodedPassword", encodedPassword);
        
        // 验证密码
        boolean matches = passwordEncoder.matches(password, encodedPassword);
        result.put("matches", matches);
        
        // 测试数据库中的密码
        String dbPassword = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi";
        boolean dbMatches = passwordEncoder.matches(password, dbPassword);
        result.put("dbPasswordMatches", dbMatches);
        
        return ResponseEntity.ok(result);
    }
}

