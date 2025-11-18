package com.gxj.cropyield.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Utility class that resolves the real client IP address from an HTTP request.
 * <p>
 * It inspects a list of common proxy headers and falls back to {@link HttpServletRequest#getRemoteAddr()}.
 * </p>
 */
public final class IpAddressResolver {

    private static final String UNKNOWN = "unknown";
    private static final List<String> IP_HEADER_CANDIDATES = List.of(
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_CLIENT_IP",
        "HTTP_X_FORWARDED_FOR"
    );

    private IpAddressResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String candidate = request.getHeader(header);
            String ip = extractIp(candidate);
            if (ip != null) {
                return normalizeIp(ip);
            }
        }
        return normalizeIp(request.getRemoteAddr());
    }

    private static String extractIp(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String[] segments = candidate.split(",");
        for (String segment : segments) {
            String ip = segment.trim();
            if (StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return null;
    }

    private static String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "UNKNOWN";
        }
        String trimmed = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(trimmed) || "::1".equals(trimmed)) {
            return "127.0.0.1";
        }
        return trimmed;
    }
}
