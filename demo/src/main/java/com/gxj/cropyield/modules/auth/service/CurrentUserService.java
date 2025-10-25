package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.entity.User;

import java.util.Set;

/**
 * 认证与账号模块的业务接口（接口），定义当前登录用户相关的辅助操作。
 */
public interface CurrentUserService {

    User getCurrentUser();

    Set<String> getCurrentUserRoleCodes();

    boolean hasRole(String roleCode);
}
