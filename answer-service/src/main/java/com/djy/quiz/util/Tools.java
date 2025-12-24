package com.djy.quiz.util;

import com.djy.quiz.constant.RoleConstant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class Tools {
    /**
     * 管理员鉴权校验（抛出异常由全局处理器返回错误）
     */
    public boolean checkAdmin(HttpServletRequest request) {
//        Integer role = (Integer) request.getAttribute("role");
//        if (role == null || role != RoleConstant.ADMIN) {
//            return false;
//        }
//        return true;
        if(getUserRole().equals("1")){
            System.out.println("检查admin：user-role:"+getUserRole());
            return true;
        } else  {
            return false;
        }
    }

    public boolean checkSource(HttpServletRequest request) {
        String repackaged = (String) request.getAttribute("repackaged");

        if (repackaged == null || !repackaged .equals( "true")) {
            System.out.println("repackaged != \"true\":  "+repackaged);
            return false;
        }
        System.out.println("repackaged == \"true\":  "+repackaged);
        return true;
    }
    public static Long getUserId() {
        HttpServletRequest request = getRequest();
        String userId = request.getHeader("X-User-Id");
        return userId != null ? Long.parseLong(userId) : null;
    }

    /**
     * 获取用户名
     */
    public static String getUserName() {
        HttpServletRequest request = getRequest();
        return request.getHeader("X-User-Name");
    }

    /**
     * 获取用户角色
     */
    public static String getUserRole() {
        HttpServletRequest request = getRequest();
        return request.getHeader("X-User-Role");
    }

    /**
     * 获取Token来源
     */
    public static String getTokenSource() {
        HttpServletRequest request = getRequest();
        return request.getHeader("X-Token-Source");
    }

    /**
     * 是否重新包装的Token
     */
    public static Boolean isTokenRepackaged() {
        HttpServletRequest request = getRequest();
        String repackaged = request.getHeader("X-Token-Repackaged");
        return repackaged != null ? Boolean.parseBoolean(repackaged) : false;
    }

    /**
     * 获取原始Authorization头
     */
    public static String getAuthorization() {
        HttpServletRequest request = getRequest();
        return request.getHeader("Authorization");
    }

    /**
     * 获取所有用户信息
     */
    public static UserInfo getUserInfo() {
        return UserInfo.builder()
                .userId(getUserId())
                .userName(getUserName())
                .role(getUserRole())
                .tokenSource(getTokenSource())
                .repackaged(isTokenRepackaged())
                .build();
    }

    /**
     * 验证用户是否已登录
     */
    public static boolean isAuthenticated() {
        return getUserId() != null;
    }

    /**
     * 验证用户是否有指定角色
     */
    public static boolean hasRole(String role) {
        String userRole = getUserRole();
        return userRole != null && userRole.equals(role);
    }

    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No current HTTP request");
        }
        return attributes.getRequest();
    }

    /**
     * 用户信息DTO
     */
    @Data
    @Builder
    public static class UserInfo {
        private Long userId;
        private String userName;
        private String role;
        private String tokenSource;
        private Boolean repackaged;
    }
}
