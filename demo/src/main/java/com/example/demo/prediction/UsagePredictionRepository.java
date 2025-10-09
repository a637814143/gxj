package com.example.demo.prediction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsagePredictionRepository extends JpaRepository<UsagePrediction, Long> {

    @Query("select count(distinct p.software.id) from UsagePrediction p")
    long countDistinctSoftware();

    List<UsagePrediction> findByPredictionDateBetween(LocalDate start, LocalDate end);

    List<UsagePrediction> findTop5ByOrderByPredictionDateDesc();

    Optional<UsagePrediction> findTop1BySoftwareIdOrderByPredictionDateDesc(Long softwareId);

    boolean existsBySoftwareId(Long softwareId);

    @Query("select max(p.predictionDate) from UsagePrediction p")
    LocalDate findLatestPredictionDate();
}
