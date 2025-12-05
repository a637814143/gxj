package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ForecastModelDepartmentPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastModelDepartmentPolicyRepository extends JpaRepository<ForecastModelDepartmentPolicy, Long> {
    Optional<ForecastModelDepartmentPolicy> findByDepartmentCodeIgnoreCase(String departmentCode);
}
