package com.itheima.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.annotation.Log;
import com.itheima.annotation.LogType;
import com.itheima.annotation.Monitor;
import com.itheima.pojo.OperationLog;
import com.itheima.service.OperationLogService;
import com.itheima.utils.RequestUtils;
import com.itheima.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 日志切面
 * 处理@Log注解的操作日志记录和@Monitor注解的性能监控
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    // 敏感字段列表（需要脱敏）
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "pwd", "token", "secret", "key", "credential",
            "phone", "mobile", "email", "idCard", "bankCard"
    );

    /**
     * 处理@Log注解的操作日志
     */
    @Around("@annotation(com.itheima.annotation.Log)")
    public Object recordOperationLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);

        // 获取请求信息
        HttpServletRequest request = getRequest();
        String ip = request != null ? RequestUtils.getClientIp() : "unknown";
        String url = request != null ? request.getRequestURI() : "unknown";
        String method1 = request != null ? request.getMethod() : "unknown";

        // 获取用户信息
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = claims != null ? (Integer) claims.get("id") : null;
        String username = claims != null ? (String) claims.get("username") : "anonymous";

        // 构建日志对象
        OperationLog operationLog = new OperationLog();
        operationLog.setModule(logAnnotation.module());
        operationLog.setOperation(logAnnotation.operation());
        operationLog.setType(logAnnotation.type().name());
        operationLog.setMethod(method1);
        operationLog.setRequestUrl(url);
        operationLog.setRequestIp(ip);
        operationLog.setUserId(userId);
        operationLog.setUsername(username);
        operationLog.setCreateTime(LocalDateTime.now());

        // 记录请求参数
        if (logAnnotation.recordParams()) {
            try {
                String params = getRequestParams(joinPoint, logAnnotation.excludeParams());
                if (logAnnotation.desensitize()) {
                    params = desensitize(params);
                }
                operationLog.setRequestParams(params);
            } catch (Exception e) {
                log.warn("记录请求参数失败: {}", e.getMessage());
            }
        }

        Object result = null;
        Exception exception = null;

        try {
            // 执行方法
            result = joinPoint.proceed();
            operationLog.setStatus("success");

            // 记录响应结果
            if (logAnnotation.recordResult() && result != null) {
                try {
                    String resultJson = objectMapper.writeValueAsString(result);
                    if (resultJson.length() > 2000) {
                        resultJson = resultJson.substring(0, 2000) + "...";
                    }
                    operationLog.setResponseResult(resultJson);
                } catch (Exception e) {
                    log.warn("记录响应结果失败: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            exception = e;
            operationLog.setStatus("fail");
            operationLog.setErrorMsg(e.getMessage());
            throw e;

        } finally {
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            operationLog.setDuration((int) (endTime - startTime));

            // 异步保存日志
            saveLogAsync(operationLog);

            // 控制台日志
            log.info("操作日志: module={}, operation={}, userId={}, ip={}, duration={}ms, status={}",
                    operationLog.getModule(), operationLog.getOperation(),
                    operationLog.getUserId(), operationLog.getRequestIp(),
                    operationLog.getDuration(), operationLog.getStatus());
        }

        return result;
    }

    /**
     * 处理@Monitor注解的性能监控
     */
    @Around("@annotation(com.itheima.annotation.Monitor)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Monitor monitor = method.getAnnotation(Monitor.class);

        String monitorName = monitor.name().isEmpty() ?
                method.getDeclaringClass().getSimpleName() + "." + method.getName() :
                monitor.name();

        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            return result;

        } catch (Exception e) {
            exception = e;
            throw e;

        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 记录性能日志
            if (duration >= monitor.threshold()) {
                log.warn("慢接口告警: name={}, duration={}ms, threshold={}ms",
                        monitorName, duration, monitor.threshold());

                // 可以在这里添加告警通知逻辑
                // alertService.sendSlowApiAlert(monitorName, duration, monitor.threshold());
            } else {
                log.debug("接口性能监控: name={}, duration={}ms", monitorName, duration);
            }

            // 记录详细性能信息
            if (monitor.detailed()) {
                log.info("接口详细性能: name={}, duration={}ms, class={}, method={}",
                        monitorName, duration,
                        method.getDeclaringClass().getSimpleName(),
                        method.getName());
            }

            // 可以将性能数据存入Redis或数据库，用于后续分析
            // performanceService.record(monitorName, duration, exception == null);
        }
    }

    /**
     * 异步保存日志
     */
    @Async
    public void saveLogAsync(OperationLog operationLog) {
        try {
            operationLogService.save(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(ProceedingJoinPoint joinPoint, String[] excludeParams) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || args == null) {
            return "{}";
        }

        Set<String> excludeSet = Set.of(excludeParams);
        Map<String, Object> params = new HashMap<>();

        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i];
            Object paramValue = args[i];

            // 排除指定参数
            if (excludeSet.contains(paramName)) {
                continue;
            }

            // 排除文件上传参数
            if (paramValue instanceof MultipartFile) {
                params.put(paramName, "[FILE]");
                continue;
            }

            // 排除HttpServletRequest和HttpServletResponse
            if (paramValue instanceof HttpServletRequest ||
                paramValue instanceof jakarta.servlet.http.HttpServletResponse) {
                continue;
            }

            try {
                params.put(paramName, paramValue);
            } catch (Exception e) {
                params.put(paramName, "[无法序列化]");
            }
        }

        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return params.toString();
        }
    }

    /**
     * 敏感信息脱敏
     */
    private String desensitize(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        String result = json;
        for (String field : SENSITIVE_FIELDS) {
            // 脱敏模式：保留前3位，其余用*代替
            result = result.replaceAll(
                    "(\"" + field + "\"\\s*:\\s*\")([^\"]+)(\")",
                    "$1***$3"
            );
            // 数字类型的字段
            result = result.replaceAll(
                    "(\"" + field + "\"\\s*:\\s*)(\\d+)",
                    "$1***"
            );
        }

        return result;
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
