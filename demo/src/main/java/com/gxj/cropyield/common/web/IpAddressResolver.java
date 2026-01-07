package com.gxj.cropyield.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

/**
 主要用于审计日志和安全记录，获取客户端IP地址。
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
        if (isLoopback(trimmed)) {
            return resolveNonLoopbackLocalAddress();
        }
        return trimmed;
    }

    private static boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip);
    }

    private static String resolveNonLoopbackLocalAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address inet4Address) {
                        return inet4Address.getHostAddress();
                    }
                }
            }
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost != null && !localHost.isLoopbackAddress()) {
                return localHost.getHostAddress();
            }
        } catch (SocketException ignored) {
            // ignore and fall through to default value
        } catch (Exception ignored) {
            // ignore and fall through to default value
        }
        return "127.0.0.1";
    }
}
