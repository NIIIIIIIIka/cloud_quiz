package com.djy.quiz.util;

import com.djy.quiz.constant.RoleConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class Tools {
    /**
     * 管理员鉴权校验（抛出异常由全局处理器返回错误）
     */
    public boolean checkAdmin(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != RoleConstant.ADMIN) {
            return false;
        }
        return true;
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

}
