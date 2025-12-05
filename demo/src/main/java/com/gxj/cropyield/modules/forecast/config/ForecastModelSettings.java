package com.gxj.cropyield.modules.forecast.config;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "forecast.model")
public class ForecastModelSettings {

    /**
     * 允许业务侧创建与使用的模型类型，管理员可通过配置限制类型范围。
     */
    private Set<ForecastModel.ModelType> allowedTypes = EnumSet.of(
            ForecastModel.ModelType.TIME_SERIES,
            ForecastModel.ModelType.MACHINE_LEARNING
    );

    public Set<ForecastModel.ModelType> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(Set<ForecastModel.ModelType> allowedTypes) {
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            this.allowedTypes = EnumSet.allOf(ForecastModel.ModelType.class);
        } else {
            this.allowedTypes = EnumSet.copyOf(allowedTypes);
        }
    }

    public boolean isTypeAllowed(ForecastModel.ModelType type) {
        return type != null && allowedTypes.contains(type);
    }
}
