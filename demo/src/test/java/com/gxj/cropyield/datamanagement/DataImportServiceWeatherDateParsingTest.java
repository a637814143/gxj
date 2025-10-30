package com.gxj.cropyield.datamanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.repository.DatasetFileRepository;
import com.gxj.cropyield.datamanagement.repository.DataImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataImportServiceWeatherDateParsingTest {

    private DataImportService service;

    @BeforeEach
    void setUp() {
        service = new DataImportService(
                Mockito.mock(CropRepository.class),
                Mockito.mock(RegionRepository.class),
                Mockito.mock(DatasetFileRepository.class),
                Mockito.mock(DataImportJobRepository.class),
                Mockito.mock(JdbcTemplate.class),
                new ObjectMapper()
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    void parsesIsoDateFromSampleCsv() throws Exception {
        Method parseCsv = DataImportService.class.getDeclaredMethod("parseCsv", Path.class);
        parseCsv.setAccessible(true);
        List<?> records = (List<?>) parseCsv.invoke(service, resolveSampleCsv());
        assertThat(records).isNotEmpty();
        Object first = records.get(0);
        Method valuesAccessor = first.getClass().getDeclaredMethod("values");
        valuesAccessor.setAccessible(true);
        Map<String, String> values = (Map<String, String>) valuesAccessor.invoke(first);
        assertThat(values).containsKey("recordDate");
        assertThat(values.get("recordDate")).isEqualTo("2009-01-01");

        Method parseWeatherDate = DataImportService.class.getDeclaredMethod("parseWeatherDate", String.class);
        parseWeatherDate.setAccessible(true);
        LocalDate parsed = (LocalDate) parseWeatherDate.invoke(service, values.get("recordDate"));
        assertThat(parsed).isEqualTo(LocalDate.of(2009, 1, 1));
    }

    private Path resolveSampleCsv() {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && current != null; depth++) {
            Path candidate = current.resolve("docs/weather-import-sample.csv");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法找到天气示例数据文件");
    }
}
