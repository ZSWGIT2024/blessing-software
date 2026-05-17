package com.itheima.aspect;

import com.itheima.annotation.RequirePermission;
import com.itheima.exception.PermissionDeniedException;
import com.itheima.service.impl.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限校验切面
 * 拦截带有@RequirePermission或@RequireAdmin注解的方法，进行权限校验
 */
@Slf4j
@Aspect
@Component
@Order(1)  // 优先级高于日志切面
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    /**
     * 处理@RequirePermission注解
     */
    @Around("@annotation(com.itheima.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        String permission = annotation.value();
        String description = annotation.description();
        boolean checkOwnership = annotation.checkOwnership();

        log.debug("权限校验：permission={}, description={}", permission, description);

        // 检查权限
        if (!permissionService.hasPermission(permission)) {
            log.warn("权限不足：required={}, userId={}", permission, permissionService.getCurrentUserId());
            throw new PermissionDeniedException(permission, "权限不足：" + (description.isEmpty() ? permission : description));
        }

        // 如果需要所有权校验
        if (checkOwnership) {
            // 从方法参数中获取资源用户ID（通常在第二个参数）
            Object[] args = joinPoint.getArgs();
            if (args.length >= 2 && args[1] instanceof Integer) {
                Integer resourceUserId = (Integer) args[1];
                if (!permissionService.checkOwnership(resourceUserId)) {
                    log.warn("资源所有权校验失败：resourceUserId={}, currentUserId={}",
                            resourceUserId, permissionService.getCurrentUserId());
                    throw new PermissionDeniedException("无权操作此资源");
                }
            }
        }

        return joinPoint.proceed();
    }

    /**
     * 处理@RequireAdmin注解
     */
    @Around("@annotation(com.itheima.annotation.RequireAdmin)")
    public Object checkAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("管理员权限校验");

        if (!permissionService.isAdmin()) {
            log.warn("非管理员访问：userId={}", permissionService.getCurrentUserId());
            throw new PermissionDeniedException("需要管理员权限");
        }

        return joinPoint.proceed();
    }
}
