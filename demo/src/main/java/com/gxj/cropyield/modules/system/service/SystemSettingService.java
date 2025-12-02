package com.gxj.cropyield.modules.system.service;

import com.gxj.cropyield.modules.system.dto.SystemSettingRequest;
import com.gxj.cropyield.modules.system.dto.SystemSettingResponse;
import com.gxj.cropyield.modules.system.dto.PublicNoticeResponse;
/**
 * 系统设置模块的业务接口（接口），定义系统设置相关的核心业务操作。
 * <p>核心方法：getCurrentSettings、updateSettings。</p>
 */

public interface SystemSettingService {

    SystemSettingResponse getCurrentSettings();

    SystemSettingResponse updateSettings(SystemSettingRequest request);

    PublicNoticeResponse getPublicNotice();
}
