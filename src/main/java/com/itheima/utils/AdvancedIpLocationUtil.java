package com.itheima.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AdvancedIpLocationUtil {

    private static final String IPAPI_URL = "http://ip-api.com/json/%s?lang=zh-CN";
    private static final Map<String, String> CACHE = new HashMap<>();

    /**
     * 使用ip-api.com获取详细地理位置（免费版限制45次/分钟）
     */
    public Map<String, String> getDetailedLocation(String ip) {
        Map<String, String> result = new HashMap<>();

        try {
            if (isLocalOrInternalIp(ip)) {
                result.put("country", "本地");
                result.put("region", "局域网");
                result.put("city", "内网");
                return result;
            }

            RestTemplate restTemplate = new RestTemplate();
            String url = String.format(IPAPI_URL, ip);
            String response = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            if ("success".equals(root.get("status").asText())) {
                result.put("country", root.get("country").asText());
                result.put("region", root.get("regionName").asText());
                result.put("city", root.get("city").asText());
                result.put("isp", root.get("isp").asText());
                result.put("lat", root.get("lat").asText());
                result.put("lon", root.get("lon").asText());
            }
        } catch (Exception e) {
            log.warn("获取IP详细地理位置失败: ip={}", ip, e);
        }

        return result;
    }

    private boolean isLocalOrInternalIp(String ip) {
        // 同之前的判断逻辑
        return ip.equals("127.0.0.1") || ip.startsWith("192.168.") ||
                ip.startsWith("10.") || ip.startsWith("172.16.");
    }
}