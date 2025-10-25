package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * 预测管理模块的数据访问接口（接口），封装了对预测管理相关数据表的持久化操作。
 */

public interface ForecastModelRepository extends JpaRepository<ForecastModel, Long> {
    Optional<ForecastModel> findByName(String name);

    Optional<ForecastModel> findByType(ForecastModel.ModelType type);
}
