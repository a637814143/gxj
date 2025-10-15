package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.entity.RefreshToken;
import com.gxj.cropyield.modules.auth.entity.User;

public interface RefreshTokenService {

    RefreshToken create(User user);

    RefreshToken validate(String token);

    RefreshToken rotate(RefreshToken token);
}
