package com.gxj.cropyield.modules.base.repository;

import com.gxj.cropyield.modules.base.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("cropyieldCropRepository")
public interface CropRepository extends JpaRepository<Crop, Long> {
    Optional<Crop> findByCode(String code);

    Optional<Crop> findByNameIgnoreCase(String name);
}
