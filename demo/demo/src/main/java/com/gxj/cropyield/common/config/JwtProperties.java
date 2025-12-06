package com.gxj.cropyield.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * 全局配置模块的配置类，配置全局配置相关的基础设施与框架行为。
 * <p>核心方法：getSecret、setSecret、getExpiration、setExpiration、getRefreshExpiration、setRefreshExpiration。</p>
 */

@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret;

    private long expiration;

    private long refreshExpiration = 604800000;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setRefreshExpiration(long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
}
