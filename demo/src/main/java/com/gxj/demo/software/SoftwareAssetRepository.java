package com.gxj.demo.software;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SoftwareAssetRepository extends JpaRepository<SoftwareAsset, Long> {

    List<SoftwareAsset> findByMaintenanceExpiryDateBetweenOrderByMaintenanceExpiryDate(LocalDate start, LocalDate end);
}
