package com.itheima.utils;
import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    private static final String[] IP_HEADERS = {
            "X-Real-IP",
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    public static String getClientIp(HttpServletRequest request) {
        // 遍历所有可能的IP头
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // 处理多个IP的情况
                if (ip.contains(",")) {
                    String[] ips = ip.split(",");
                    for (String subIp : ips) {
                        if (isValidIp(subIp.trim())) {
                            return subIp.trim();
                        }
                    }
                }
                return ip;
            }
        }

        // 如果都没有，使用远程地址
        String remoteAddr = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(remoteAddr) ? "127.0.0.1" : remoteAddr;
    }

    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        // 简单验证IP格式
        return ip.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$") ||
                ip.matches("^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$");
    }

    /**
     * 获取IP归属地（需要第三方库，如ip2region）
     */
    public static String getIpLocation(String ip) {
        // 这里可以集成IP地理位置查询
        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "内网IP";
        }
        return "未知";
    }
}
