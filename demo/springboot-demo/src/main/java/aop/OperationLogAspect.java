package com.example.springbootdemo.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final int MAX_ARGS_LENGTH = 200;

    @Around("execution(* com.example.springbootdemo.controller..*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String args = truncate(Arrays.toString(joinPoint.getArgs()), MAX_ARGS_LENGTH);
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - startTime;
            log.info("[OperationLog] method={}, args={}, cost={}ms", methodName, args, cost);
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("[OperationLog] method={}, args={}, cost={}ms, error={}", methodName, args, cost, e.getMessage());
            throw e;
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLen) {
            return str;
        }
        return str.substring(0, maxLen) + "...(truncated)";
    }
}
