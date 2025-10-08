package com.dali.cropyield.modules.auth.service;

import com.dali.cropyield.modules.auth.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> findAll();

    Optional<User> findByUsername(String username);

    User save(User user);
}
