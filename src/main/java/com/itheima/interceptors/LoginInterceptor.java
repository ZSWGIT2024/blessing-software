package com.itheima.interceptors;

import com.itheima.pojo.Result;
import com.itheima.utils.JwtTokenService;
import com.itheima.utils.JwtUtil;
import com.itheima.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private final JwtTokenService jwtTokenService;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取令牌token
        String token = request.getHeader("Authorization");
        try {
            // 1. Token不能为空
            if (token == null || token.trim().isEmpty()) {
                throw new RuntimeException("Token为空");
            }

            // 2. 检查Token是否在黑名单中（已登出）
            if (jwtTokenService.isBlacklisted(token)) {
                throw new RuntimeException("Token已失效");
            }

            // 3. 验证Token并解析
            Map<String, Object> claims = jwtUtil.parseToken(token);
            if (claims == null) {
                throw new RuntimeException("Token解析失败");
            }

            // 4. 检查用户状态
            String status = (String) claims.get("status");
            if ("banned".equals(status)) {
                throw new RuntimeException("用户已被封禁");
            }

            // 5. 把业务数据放入ThreadLocal中
            ThreadLocalUtil.set(claims);
            return true;

        } catch (Exception e) {
            //HTTP响应状态码为401
            response.setStatus(401);
            //不放行
            return false;
        }
    }


    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清除ThreadLocal中的数据
        ThreadLocalUtil.remove();
    }
}
