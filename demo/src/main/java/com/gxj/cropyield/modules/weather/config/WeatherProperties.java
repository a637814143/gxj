package com.gxj.cropyield.modules.weather.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 天气服务的属性配置，封装和风天气的访问密钥、基础地址以及缓存策略。
 */
@ConfigurationProperties(prefix = "weather")
public class WeatherProperties {


    private Duration cacheTtl = Duration.ofMinutes(2);


    private final QWeatherProperties qweather = new QWeatherProperties();

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public QWeatherProperties getQweather() {
        return qweather;
    }


    public static class QWeatherProperties {


        private String baseUrl = "https://m776x8rde7.re.qweatherapi.com/v7";


        private String airQualityBaseUrl;


        private String geoBaseUrl;


        private String key;


        private String token;


        private String language = "zh";


        private String unit = "m";


        private java.util.Map<String, String> regionLocations = new java.util.HashMap<>();


        private String defaultLocation;


        private Duration connectTimeout = Duration.ofSeconds(2);


        private Duration readTimeout = Duration.ofSeconds(5);


        private String referer;


        private String userAgent;


        private AuthMode authMode = AuthMode.QUERY_KEY;


        private LocationMode locationMode = LocationMode.COORDINATE;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getAirQualityBaseUrl() {
            return airQualityBaseUrl;
        }

        public void setAirQualityBaseUrl(String airQualityBaseUrl) {
            this.airQualityBaseUrl = airQualityBaseUrl;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public java.util.Map<String, String> getRegionLocations() {
            return regionLocations;
        }

        public void setRegionLocations(java.util.Map<String, String> regionLocations) {
            this.regionLocations = regionLocations;
        }

        public String getDefaultLocation() {
            return defaultLocation;
        }

        public void setDefaultLocation(String defaultLocation) {
            this.defaultLocation = defaultLocation;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public String getGeoBaseUrl() {
            return geoBaseUrl;
        }

        public void setGeoBaseUrl(String geoBaseUrl) {
            this.geoBaseUrl = geoBaseUrl;
        }

        public String getReferer() {
            return referer;
        }

        public void setReferer(String referer) {
            this.referer = referer;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public AuthMode getAuthMode() {
            return authMode;
        }

        public void setAuthMode(AuthMode authMode) {
            this.authMode = authMode;
        }

        public LocationMode getLocationMode() {
            return locationMode;
        }

        public void setLocationMode(LocationMode locationMode) {
            this.locationMode = locationMode;
        }

        public enum LocationMode {
            COORDINATE,
            GEO_LOOKUP
        }

        public enum AuthMode {
            QUERY_KEY,
            HEADER_BEARER
        }
    }
}
