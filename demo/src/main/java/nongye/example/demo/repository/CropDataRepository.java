package nongye.example.demo.repository;

import nongye.example.demo.entity.CropData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CropDataRepository extends JpaRepository<CropData, Long> {
    
    List<CropData> findByRegionCode(String regionCode);
    
    List<CropData> findByCropType(String cropType);
    
    List<CropData> findByYear(Integer year);
    
    List<CropData> findByRegionCodeAndYear(String regionCode, Integer year);
    
    List<CropData> findByCropTypeAndYear(String cropType, Integer year);
    
    List<CropData> findByRegionCodeAndCropType(String regionCode, String cropType);
    
    @Query("SELECT c FROM CropData c WHERE c.regionCode = :regionCode AND c.cropType = :cropType AND c.year BETWEEN :startYear AND :endYear ORDER BY c.year")
    List<CropData> findByRegionAndCropAndYearRange(@Param("regionCode") String regionCode, 
                                                  @Param("cropType") String cropType,
                                                  @Param("startYear") Integer startYear,
                                                  @Param("endYear") Integer endYear);
    
    @Query("SELECT DISTINCT c.regionCode FROM CropData c")
    List<String> findDistinctRegionCodes();
    
    @Query("SELECT DISTINCT c.cropType FROM CropData c")
    List<String> findDistinctCropTypes();
    
    @Query("SELECT DISTINCT c.year FROM CropData c ORDER BY c.year")
    List<Integer> findDistinctYears();
    
    @Query("SELECT AVG(c.yield) FROM CropData c WHERE c.regionCode = :regionCode AND c.cropType = :cropType AND c.year BETWEEN :startYear AND :endYear")
    BigDecimal findAverageYield(@Param("regionCode") String regionCode, 
                               @Param("cropType") String cropType,
                               @Param("startYear") Integer startYear,
                               @Param("endYear") Integer endYear);
    
    @Query("SELECT SUM(c.yield) FROM CropData c WHERE c.regionCode = :regionCode AND c.year = :year")
    BigDecimal findTotalYieldByRegionAndYear(@Param("regionCode") String regionCode, 
                                           @Param("year") Integer year);
    
    @Query("SELECT c FROM CropData c WHERE c.regionCode IN :regionCodes AND c.year = :year")
    List<CropData> findByRegionCodesAndYear(@Param("regionCodes") List<String> regionCodes, 
                                           @Param("year") Integer year);
    
    List<CropData> findByRegionCodeAndCropNameAndYear(String regionCode, String cropName, Integer year);
}
