package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.entity.RefreshToken;
import com.gxj.cropyield.modules.auth.entity.User;
/**
 * 认证与账号模块的业务接口（接口），定义认证与账号相关的核心业务操作。
 * <p>核心方法：create、validate、rotate。</p>
 */

public interface RefreshTokenService {

    RefreshToken create(User user);

    RefreshToken validate(String token);

    RefreshToken rotate(RefreshToken token);
}
