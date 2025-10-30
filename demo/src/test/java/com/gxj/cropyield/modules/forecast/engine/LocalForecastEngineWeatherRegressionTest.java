package com.gxj.cropyield.modules.forecast.engine;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalForecastEngineWeatherRegressionTest {

    private final LocalForecastEngine engine = new LocalForecastEngine();

    @Test
    void shouldForecastUsingWeatherFeatures() {
        List<ForecastEngineRequest.HistoryPoint> history = List.of(
            new ForecastEngineRequest.HistoryPoint(
                "2020",
                3.5,
                Map.of(
                    "avgMaxTemperature", 20.0,
                    "avgMinTemperature", 10.0,
                    "avgDiurnalRange", 10.0,
                    "totalSunshineHours", 1000.0
                )
            ),
            new ForecastEngineRequest.HistoryPoint(
                "2021",
                3.7,
                Map.of(
                    "avgMaxTemperature", 21.0,
                    "avgMinTemperature", 11.0,
                    "avgDiurnalRange", 10.0,
                    "totalSunshineHours", 1050.0
                )
            ),
            new ForecastEngineRequest.HistoryPoint(
                "2022",
                3.9,
                Map.of(
                    "avgMaxTemperature", 22.0,
                    "avgMinTemperature", 12.0,
                    "avgDiurnalRange", 10.0,
                    "totalSunshineHours", 1100.0
                )
            )
        );

        ForecastEngineRequest request = new ForecastEngineRequest(
            ForecastModel.ModelType.WEATHER_REGRESSION.name(),
            "YEARLY",
            2,
            history,
            Map.of("historyYears", 3)
        );

        ForecastEngineResponse response = engine.forecast(request);

        assertEquals(2, response.forecast().size());
        assertNotNull(response.metrics());
        assertNotNull(response.metrics().mae());
        assertEquals(4.1, response.forecast().get(0).value(), 0.3);
        assertEquals(4.3, response.forecast().get(1).value(), 0.3);
    }
}
