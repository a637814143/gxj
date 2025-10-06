package nongye.example.demo.service;

import nongye.example.demo.entity.CropData;
import nongye.example.demo.repository.CropDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CropDataService {
    
    @Autowired
    private CropDataRepository cropDataRepository;
    
    public CropData saveCropData(CropData cropData) {
        return cropDataRepository.save(cropData);
    }
    
    public List<CropData> saveAllCropData(List<CropData> cropDataList) {
        return cropDataRepository.saveAll(cropDataList);
    }
    
    public CropData getCropDataById(Long id) {
        return cropDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("作物数据不存在"));
    }
    
    public List<CropData> getAllCropData() {
        return cropDataRepository.findAll();
    }
    
    public List<CropData> getCropDataByRegion(String regionCode) {
        return cropDataRepository.findByRegionCode(regionCode);
    }
    
    public List<CropData> getCropDataByCropType(String cropType) {
        return cropDataRepository.findByCropType(cropType);
    }
    
    public List<CropData> getCropDataByYear(Integer year) {
        return cropDataRepository.findByYear(year);
    }
    
    public List<CropData> getCropDataByRegionAndYear(String regionCode, Integer year) {
        return cropDataRepository.findByRegionCodeAndYear(regionCode, year);
    }
    
    public List<CropData> getCropDataByRegionAndCropType(String regionCode, String cropType) {
        return cropDataRepository.findByRegionCodeAndCropType(regionCode, cropType);
    }
    
    public List<CropData> getCropDataByRegionAndCropAndYearRange(String regionCode, String cropType, 
                                                               Integer startYear, Integer endYear) {
        return cropDataRepository.findByRegionAndCropAndYearRange(regionCode, cropType, startYear, endYear);
    }
    
    public List<String> getDistinctRegionCodes() {
        return cropDataRepository.findDistinctRegionCodes();
    }
    
    public List<String> getDistinctCropTypes() {
        return cropDataRepository.findDistinctCropTypes();
    }
    
    public List<Integer> getDistinctYears() {
        return cropDataRepository.findDistinctYears();
    }
    
    public BigDecimal getAverageYield(String regionCode, String cropType, Integer startYear, Integer endYear) {
        return cropDataRepository.findAverageYield(regionCode, cropType, startYear, endYear);
    }
    
    public BigDecimal getTotalYieldByRegionAndYear(String regionCode, Integer year) {
        return cropDataRepository.findTotalYieldByRegionAndYear(regionCode, year);
    }
    
    public List<CropData> getCropDataByRegionCodesAndYear(List<String> regionCodes, Integer year) {
        return cropDataRepository.findByRegionCodesAndYear(regionCodes, year);
    }
    
    public void deleteCropData(Long id) {
        if (!cropDataRepository.existsById(id)) {
            throw new RuntimeException("作物数据不存在");
        }
        cropDataRepository.deleteById(id);
    }
    
    public void deleteCropDataByRegionAndYear(String regionCode, Integer year) {
        List<CropData> cropDataList = cropDataRepository.findByRegionCodeAndYear(regionCode, year);
        cropDataRepository.deleteAll(cropDataList);
    }
    
    // 数据统计方法
    public Map<String, BigDecimal> getYieldStatisticsByRegion(String regionCode, Integer year) {
        List<CropData> cropDataList = cropDataRepository.findByRegionCodeAndYear(regionCode, year);
        return cropDataList.stream()
                .collect(Collectors.groupingBy(
                    CropData::getCropType,
                    Collectors.reducing(BigDecimal.ZERO, CropData::getYield, BigDecimal::add)
                ));
    }
    
    public Map<Integer, BigDecimal> getYieldTrendByRegionAndCrop(String regionCode, String cropType, 
                                                               Integer startYear, Integer endYear) {
        List<CropData> cropDataList = cropDataRepository.findByRegionAndCropAndYearRange(regionCode, cropType, startYear, endYear);
        return cropDataList.stream()
                .collect(Collectors.groupingBy(
                    CropData::getYear,
                    Collectors.reducing(BigDecimal.ZERO, CropData::getYield, BigDecimal::add)
                ));
    }
    
    public Map<String, BigDecimal> getAreaStatisticsByRegion(String regionCode, Integer year) {
        List<CropData> cropDataList = cropDataRepository.findByRegionCodeAndYear(regionCode, year);
        return cropDataList.stream()
                .collect(Collectors.groupingBy(
                    CropData::getCropType,
                    Collectors.reducing(BigDecimal.ZERO, CropData::getPlantingArea, BigDecimal::add)
                ));
    }
}
