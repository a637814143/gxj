package com.gxj.cropyield.modules.weather.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 天气服务的属性配置，封装和风天气的访问密钥、基础地址以及缓存策略。
 */
@ConfigurationProperties(prefix = "weather")
public class WeatherProperties {

    /**
     * 天气数据缓存存活时间，默认 2 分钟。
     */
    private Duration cacheTtl = Duration.ofMinutes(2);

    /**
     * 和风天气平台配置。
     */
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

    /**
     * 和风天气开放平台属性集合。
     */
    public static class QWeatherProperties {

        /**
         * 和风天气接口基础地址。
         */
        private String baseUrl = "https://m776x8rde7.re.qweatherapi.com/v7";

        /**
         * 和风天气访问密钥。
         */
        private String key;

        /**
         * 默认查询语言。
         */
        private String language = "zh";

        /**
         * 温度单位，m 表示摄氏度。
         */
        private String unit = "m";

        /**
         * 区域编码到经纬度的映射，形如 "code": "经度,纬度"。
         */
        private java.util.Map<String, String> regionLocations = new java.util.HashMap<>();

        /**
         * 默认经纬度，当区域映射缺失时兜底使用。
         */
        private String defaultLocation;

        /**
         * HTTP 连接超时时间。
         */
        private Duration connectTimeout = Duration.ofSeconds(2);

        /**
         * HTTP 读取超时时间。
         */
        private Duration readTimeout = Duration.ofSeconds(5);

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
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
    }
}
