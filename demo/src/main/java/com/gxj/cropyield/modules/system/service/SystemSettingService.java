package com.gxj.cropyield.modules.system.service;

import com.gxj.cropyield.modules.system.dto.SystemSettingRequest;
import com.gxj.cropyield.modules.system.dto.SystemSettingResponse;

public interface SystemSettingService {

    SystemSettingResponse getCurrentSettings();

    SystemSettingResponse updateSettings(SystemSettingRequest request);
}
