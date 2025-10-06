package nongye.example.demo.repository;

import nongye.example.demo.entity.PredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {
    
    List<PredictionResult> findByModelId(Long modelId);
    
    List<PredictionResult> findByRegionCode(String regionCode);
    
    List<PredictionResult> findByCropType(String cropType);
    
    List<PredictionResult> findByPredictionYear(Integer predictionYear);
    
    List<PredictionResult> findByRegionCodeAndCropType(String regionCode, String cropType);
    
    List<PredictionResult> findByRegionCodeAndPredictionYear(String regionCode, Integer predictionYear);
    
    @Query("SELECT p FROM PredictionResult p WHERE p.regionCode = :regionCode AND p.cropType = :cropType AND p.predictionYear = :predictionYear")
    List<PredictionResult> findByRegionAndCropAndYear(@Param("regionCode") String regionCode, 
                                                     @Param("cropType") String cropType,
                                                     @Param("predictionYear") Integer predictionYear);
    
    @Query("SELECT p FROM PredictionResult p WHERE p.scenarioName = :scenarioName")
    List<PredictionResult> findByScenarioName(@Param("scenarioName") String scenarioName);
    
    @Query("SELECT DISTINCT p.regionCode FROM PredictionResult p")
    List<String> findDistinctRegionCodes();
    
    @Query("SELECT DISTINCT p.cropType FROM PredictionResult p")
    List<String> findDistinctCropTypes();
    
    @Query("SELECT DISTINCT p.predictionYear FROM PredictionResult p ORDER BY p.predictionYear")
    List<Integer> findDistinctPredictionYears();
}
