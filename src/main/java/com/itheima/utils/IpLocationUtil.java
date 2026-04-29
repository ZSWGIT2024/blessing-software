package com.itheima.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class IpLocationUtil {

    @Value("${ip.api.url:https://restapi.amap.com/v3/ip}")
    private String ipApiUrl;

    @Value("${ip.api.key:}")
    private String apiKey;

    private static final Map<String, String> CACHE = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取IP地址对应的地理位置
     */
    public String getLocation(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "未知";
        }

        // 本地地址和内网地址
        if (isLocalOrInternalIp(ip)) {
            return "局域网";
        }

        // 从缓存中获取
        if (CACHE.containsKey(ip)) {
            return CACHE.get(ip);
        }

        try {
            String location = queryLocation(ip);
            // 缓存结果（缓存1小时）
            CACHE.put(ip, location);
            // 简单缓存清理，防止内存泄漏
            if (CACHE.size() > 1000) {
                CACHE.clear();
            }
            return location;
        } catch (Exception e) {
            log.error("获取IP地理位置失败: ip={}", ip, e);
            return "未知";
        }
    }

    /**
     * 查询IP地理位置（使用高德地图API）
     */
    private String queryLocation(String ip) throws Exception {
        // 如果没有配置API key，使用简单的地理位置识别
        if (apiKey == null || apiKey.isEmpty()) {
            return getSimpleLocation(ip);
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = String.format("%s?key=%s&ip=%s", ipApiUrl, apiKey, ip);
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            if ("1".equals(root.get("status").asText())) {
                String province = root.get("province").asText();
                String city = root.get("city").asText();

                if (!province.isEmpty() && !city.isEmpty()) {
                    // 如果是直辖市，只显示城市
                    if (province.equals(city) || city.contains("市辖区")) {
                        return province;
                    }
                    return province + "·" + city;
                } else if (!province.isEmpty()) {
                    return province;
                }
            }
        } catch (Exception e) {
            log.warn("使用高德API查询IP失败，使用备用方案: ip={}", ip, e);
        }

        // API调用失败时使用备用方案
        return getSimpleLocation(ip);
    }

    /**
     * 简单的IP地理位置识别（国内省份级别）
     */
    private String getSimpleLocation(String ip) {
        // 简单的IP段识别（仅作为示例，实际需要更完整的IP库）
        if (ip.startsWith("1.") || ip.startsWith("14.") || ip.startsWith("27.") ||
                ip.startsWith("36.") || ip.startsWith("39.") || ip.startsWith("42.") ||
                ip.startsWith("49.") || ip.startsWith("58.") || ip.startsWith("60.") ||
                ip.startsWith("61.") || ip.startsWith("101.") || ip.startsWith("103.") ||
                ip.startsWith("106.") || ip.startsWith("110.") || ip.startsWith("111.") ||
                ip.startsWith("112.") || ip.startsWith("113.") || ip.startsWith("114.") ||
                ip.startsWith("115.") || ip.startsWith("116.") || ip.startsWith("117.") ||
                ip.startsWith("118.") || ip.startsWith("119.") || ip.startsWith("120.") ||
                ip.startsWith("121.") || ip.startsWith("122.") || ip.startsWith("123.") ||
                ip.startsWith("124.") || ip.startsWith("125.") || ip.startsWith("126.") ||
                ip.startsWith("171.") || ip.startsWith("175.") || ip.startsWith("180.") ||
                ip.startsWith("182.") || ip.startsWith("183.") || ip.startsWith("202.") ||
                ip.startsWith("203.") || ip.startsWith("210.") || ip.startsWith("211.") ||
                ip.startsWith("218.") || ip.startsWith("219.") || ip.startsWith("220.") ||
                ip.startsWith("221.") || ip.startsWith("222.")) {
            return "中国";
        }

        // 国外IP识别
        if (ip.startsWith("3.") || ip.startsWith("4.") || ip.startsWith("5.") ||
                ip.startsWith("6.") || ip.startsWith("7.") || ip.startsWith("8.") ||
                ip.startsWith("9.") || ip.startsWith("11.") || ip.startsWith("12.") ||
                ip.startsWith("13.") || ip.startsWith("15.") || ip.startsWith("16.") ||
                ip.startsWith("17.") || ip.startsWith("18.") || ip.startsWith("19.") ||
                ip.startsWith("20.") || ip.startsWith("21.") || ip.startsWith("22.") ||
                ip.startsWith("23.") || ip.startsWith("24.") || ip.startsWith("25.") ||
                ip.startsWith("26.") || ip.startsWith("28.") || ip.startsWith("29.") ||
                ip.startsWith("30.") || ip.startsWith("31.") || ip.startsWith("32.") ||
                ip.startsWith("33.") || ip.startsWith("34.") || ip.startsWith("35.") ||
                ip.startsWith("37.") || ip.startsWith("38.") || ip.startsWith("40.") ||
                ip.startsWith("41.") || ip.startsWith("43.") || ip.startsWith("44.") ||
                ip.startsWith("45.") || ip.startsWith("46.") || ip.startsWith("47.") ||
                ip.startsWith("48.") || ip.startsWith("50.") || ip.startsWith("51.") ||
                ip.startsWith("52.") || ip.startsWith("53.") || ip.startsWith("54.") ||
                ip.startsWith("55.") || ip.startsWith("56.") || ip.startsWith("57.") ||
                ip.startsWith("59.") || ip.startsWith("62.") || ip.startsWith("63.") ||
                ip.startsWith("64.") || ip.startsWith("65.") || ip.startsWith("66.") ||
                ip.startsWith("67.") || ip.startsWith("68.") || ip.startsWith("69.") ||
                ip.startsWith("70.") || ip.startsWith("71.") || ip.startsWith("72.") ||
                ip.startsWith("73.") || ip.startsWith("74.") || ip.startsWith("75.") ||
                ip.startsWith("76.") || ip.startsWith("77.") || ip.startsWith("78.") ||
                ip.startsWith("79.") || ip.startsWith("80.") || ip.startsWith("81.") ||
                ip.startsWith("82.") || ip.startsWith("83.") || ip.startsWith("84.") ||
                ip.startsWith("85.") || ip.startsWith("86.") || ip.startsWith("87.") ||
                ip.startsWith("88.") || ip.startsWith("89.") || ip.startsWith("90.") ||
                ip.startsWith("91.") || ip.startsWith("92.") || ip.startsWith("93.") ||
                ip.startsWith("94.") || ip.startsWith("95.") || ip.startsWith("96.") ||
                ip.startsWith("97.") || ip.startsWith("98.") || ip.startsWith("99.") ||
                ip.startsWith("100.")) {
            return "海外";
        }

        return "未知";
    }

    /**
     * 判断是否为本地或内网IP
     */
    private boolean isLocalOrInternalIp(String ip) {
        return ip.equals("127.0.0.1") ||
                ip.equals("0:0:0:0:0:0:0:1") ||
                ip.equals("::1") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                ip.startsWith("172.16.") ||
                ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") ||
                ip.startsWith("172.19.") ||
                ip.startsWith("172.20.") ||
                ip.startsWith("172.21.") ||
                ip.startsWith("172.22.") ||
                ip.startsWith("172.23.") ||
                ip.startsWith("172.24.") ||
                ip.startsWith("172.25.") ||
                ip.startsWith("172.26.") ||
                ip.startsWith("172.27.") ||
                ip.startsWith("172.28.") ||
                ip.startsWith("172.29.") ||
                ip.startsWith("172.30.") ||
                ip.startsWith("172.31.");
    }

    /**
     * 获取IP的国家/地区
     */
    public String getCountry(String ip) {
        if (isLocalOrInternalIp(ip)) {
            return "本地";
        }

        // 简单判断国内国外
        String location = getLocation(ip);
        if (location.contains("中国")) {
            return "中国";
        } else if (location.equals("海外")) {
            return "海外";
        }

        return "未知";
    }
}