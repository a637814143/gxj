package com.gxj.cropyield.modules.weather.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.util.zip.GZIPInputStream;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.StreamUtils;

/**
 * 天气模块的配置类，负责构建调用第三方天气接口的 RestTemplate。
 */
@Configuration
@EnableConfigurationProperties(WeatherProperties.class)
public class WeatherConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WeatherConfiguration.class);
    private static final AtomicBoolean BEARER_TOKEN_MISSING_LOGGED = new AtomicBoolean(false);

    @Bean({"weatherRestTemplate", "qweatherRestTemplate"})
    public RestTemplate qweatherRestTemplate(RestTemplateBuilder builder, WeatherProperties properties) {
        WeatherProperties.QWeatherProperties qweather = properties.getQweather();
        Duration connectTimeout = qweather.getConnectTimeout();
        Duration readTimeout = qweather.getReadTimeout();
        return builder
            .setConnectTimeout(connectTimeout != null ? connectTimeout : Duration.ofSeconds(2))
            .setReadTimeout(readTimeout != null ? readTimeout : Duration.ofSeconds(5))
            .additionalInterceptors(decompressionInterceptor())
            .additionalInterceptors(headerPreparingInterceptor(qweather))
            .build();
    }

    private ClientHttpRequestInterceptor headerPreparingInterceptor(WeatherProperties.QWeatherProperties qweather) {
        return (request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            if (!headers.containsKey(HttpHeaders.ACCEPT)) {
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            }
            if (!headers.containsKey(HttpHeaders.ACCEPT_ENCODING)) {
                headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate");
            }
            if (StringUtils.hasText(qweather.getReferer()) && !headers.containsKey(HttpHeaders.REFERER)) {
                headers.set(HttpHeaders.REFERER, qweather.getReferer());
            }
            if (StringUtils.hasText(qweather.getUserAgent()) && !headers.containsKey(HttpHeaders.USER_AGENT)) {
                headers.set(HttpHeaders.USER_AGENT, qweather.getUserAgent());
            }
            if (qweather.getAuthMode() == WeatherProperties.QWeatherProperties.AuthMode.HEADER_BEARER
                && !headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                String token = qweather.getToken();
                if (StringUtils.hasText(token)) {
                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                } else if (StringUtils.hasText(qweather.getKey())
                    && BEARER_TOKEN_MISSING_LOGGED.compareAndSet(false, true)) {
                    log.warn("和风天气配置为 Bearer 模式，但未设置 token，将回退到 query key 方式");
                }
            }
            return execution.execute(request, body);
        };
    }

    private ClientHttpRequestInterceptor decompressionInterceptor() {
        return (request, body, execution) -> {
            ClientHttpResponse response = execution.execute(request, body);
            try {
                return maybeDecompress(response);
            } catch (IOException ex) {
                log.warn("解压和风天气响应失败，将返回原始响应: {}", ex.getMessage());
                return response;
            }
        };
    }

    private ClientHttpResponse maybeDecompress(ClientHttpResponse response) throws IOException {
        List<String> encodings = response.getHeaders().get(HttpHeaders.CONTENT_ENCODING);
        if (encodings == null) {
            return response;
        }
        for (String encoding : encodings) {
            if (encoding == null) {
                continue;
            }
            if (encoding.toLowerCase().contains("gzip")) {
                return new DecompressedClientHttpResponse(response, decompressBody(response));
            }
        }
        return response;
    }

    private byte[] decompressBody(ClientHttpResponse response) throws IOException {
        try (InputStream bodyStream = response.getBody();
             GZIPInputStream gzipInputStream = new GZIPInputStream(bodyStream);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            StreamUtils.copy(gzipInputStream, baos);
            return baos.toByteArray();
        }
    }

    private static final class DecompressedClientHttpResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private final byte[] body;
        private final HttpHeaders headers;

        private DecompressedClientHttpResponse(ClientHttpResponse delegate, byte[] body) throws IOException {
            this.delegate = delegate;
            this.body = body;
            this.headers = new HttpHeaders();
            this.headers.putAll(delegate.getHeaders());
            this.headers.remove(HttpHeaders.CONTENT_ENCODING);
            this.headers.setContentLength(body.length);
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return delegate.getRawStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
