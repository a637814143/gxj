package com.gxj.cropyield.modules.weather.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 天气服务的属性配置，封装彩云天气的访问密钥、基础地址以及缓存策略。
 */
@ConfigurationProperties(prefix = "weather")
public class WeatherProperties {

    /**
     * 天气数据缓存存活时间，默认 2 分钟。
     */
    private Duration cacheTtl = Duration.ofMinutes(2);

    /**
     * 彩云天气平台配置。
     */
    private final CaiyunProperties caiyun = new CaiyunProperties();

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public CaiyunProperties getCaiyun() {
        return caiyun;
    }

    /**
     * 彩云天气开放平台属性集合。
     */
    public static class CaiyunProperties {

        /**
         * 彩云天气接口基础地址。
         */
        private String baseUrl = "https://api.caiyunapp.com/v2.6";

        /**
         * 彩云天气访问令牌。
         */
        private String token;

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

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
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
