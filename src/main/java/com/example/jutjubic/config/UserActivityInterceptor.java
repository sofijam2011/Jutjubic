package com.example.jutjubic.config;

import com.example.jutjubic.service.ActiveUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserActivityInterceptor implements HandlerInterceptor {

    private final ActiveUserService activeUserService;

    public UserActivityInterceptor(ActiveUserService activeUserService) {
        this.activeUserService = activeUserService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            activeUserService.registerUserActivity(username);
        }
        
        return true;
    }
}
