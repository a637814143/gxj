package com.gxj.cropyield.modules.auth.config;

import com.gxj.cropyield.modules.auth.constant.SystemRole;
import com.gxj.cropyield.modules.auth.entity.Role;
import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.auth.repository.RoleRepository;
import com.gxj.cropyield.modules.auth.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class AuthDataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthDataInitializer(RoleRepository roleRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Role adminRole = ensureRole(SystemRole.ADMIN);
        ensureRole(SystemRole.AGRICULTURE_DEPT);
        ensureRole(SystemRole.FARMER);

        userRepository.findByUsername("admin").orElseGet(() -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFullName("系统管理员");
            admin.setRoles(Collections.singleton(adminRole));
            return userRepository.save(admin);
        });
    }

    private Role ensureRole(SystemRole role) {
        return roleRepository.findByCode(role.getCode()).orElseGet(() -> {
            Role entity = new Role();
            entity.setCode(role.getCode());
            entity.setName(role.getDisplayName());
            return roleRepository.save(entity);
        });
    }
}
