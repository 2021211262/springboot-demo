package com.example.springbootdemo.interceptor;

import com.example.springbootdemo.common.Result;
import com.example.springbootdemo.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "X-Request-Token";
    private static final String VALID_TOKEN_PREFIX = "Bearer-";

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.trim().isEmpty()) {
            writeUnauthorizedResponse(response, "请求未授权：缺少Token");
            return false;
        }
        if (!token.startsWith(VALID_TOKEN_PREFIX) || token.length() <= VALID_TOKEN_PREFIX.length()) {
            writeUnauthorizedResponse(response, "请求未授权：Token格式无效");
            return false;
        }
        return true;
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
        response.getWriter().flush();
    }
}
