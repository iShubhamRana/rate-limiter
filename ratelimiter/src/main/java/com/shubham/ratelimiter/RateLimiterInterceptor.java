package com.shubham.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;


    public RateLimiterInterceptor(RateLimiter rateLimiter){
        this.rateLimiter = rateLimiter;
    }


    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if(!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit annotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        if(annotation == null) return true;


        String user = extractUserId(request);

        String key = request.getRequestURI() + ":" + user;

        RateLimitStrategy.Result result = rateLimiter.tryAcquire(key , annotation.limit(), annotation.windowsSeconds());

        response.setHeader("X-RateLimit-Limit", String.valueOf(annotation.limit()));
        response.setHeader("Retry-After", String.valueOf(annotation.windowsSeconds()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if(!result.allowed()){
           response.setStatus(429);
           response.getWriter().write("Rate limit exceeded");
           return false;
        }

        return true;
    }

    private String extractUserId(HttpServletRequest req) {
        String userId = req.getHeader("X-User-Id");
        return userId != null ? userId : req.getRemoteAddr();
    }




}
