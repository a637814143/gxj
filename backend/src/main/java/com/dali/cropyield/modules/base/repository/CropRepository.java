package com.dali.cropyield.modules.base.repository;

import com.dali.cropyield.modules.base.entity.Crop;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropRepository extends JpaRepository<Crop, Long> {

    Optional<Crop> findByName(String name);
}
