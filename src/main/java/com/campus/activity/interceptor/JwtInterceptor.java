package com.campus.activity.interceptor;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.campus.activity.annotation.RequireRole;
import com.campus.activity.common.Result;
import com.campus.activity.common.ResultCode;
import com.campus.activity.util.JwtUtil;
import com.campus.activity.util.LoginUser;
import com.campus.activity.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录与角色校验拦截器。
 * 前端路由守卫只是"体验"，这里才是真正的安全边界。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String authorization = request.getHeader(HEADER);
        if (authorization == null || authorization.trim().isEmpty()) {
            return writeError(response, ResultCode.UNAUTHORIZED, "请先登录");
        }
        String token = authorization.startsWith(BEARER)
                ? authorization.substring(BEARER.length())
                : authorization;

        DecodedJWT jwt;
        try {
            jwt = jwtUtil.verify(token);
        } catch (JWTVerificationException e) {
            log.debug("token 校验失败: {}", e.getMessage());
            return writeError(response, ResultCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }

        LoginUser loginUser = new LoginUser(
                jwt.getClaim("userId").asInt(),
                jwt.getClaim("username").asString(),
                jwt.getClaim("role").asString());
        UserContext.set(loginUser);

        return checkRole(handler, loginUser, response);
    }

    private boolean checkRole(Object handler, LoginUser loginUser, HttpServletResponse response) throws IOException {
        HandlerMethod method = (HandlerMethod) handler;
        RequireRole requireRole = method.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = method.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }
        for (String role : requireRole.value()) {
            if (role.equals(loginUser.getRole())) {
                return true;
            }
        }
        return writeError(response, ResultCode.FORBIDDEN, "当前角色无权执行该操作");
    }

    /**
     * 统一返回 HTTP 200 + 业务码，前端 axios 拦截器只看 body 里的 code。
     */
    private boolean writeError(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, msg)));
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // ThreadLocal 必须清理，否则 Tomcat 线程复用会串用户
        UserContext.clear();
    }
}
