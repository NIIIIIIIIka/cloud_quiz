package com.djy.quiz.feign;

import com.djy.quiz.util.Tools;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {
    private final Tools tools;

    public FeignClientConfig(Tools tools) {
        this.tools = tools;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new FeignRequestInterceptor();
    }

    public class FeignRequestInterceptor implements RequestInterceptor {
        @Override
        public void apply(RequestTemplate template) {
            // 从UserContext获取用户信息
            Long userId = tools.getUserId();
            String userName = tools.getUserName();
            String userRole = tools.getUserRole();
            String tokenSource ="answer-service";
            Boolean repackaged = tools.isTokenRepackaged();
            String authorization = tools.getAuthorization();

            System.out.println("Feign调用 - 用户信息: userId=" + userId +
                    ", userName=" + userName + ", role=" + userRole);

            // 传递原始Authorization头
            if (authorization != null && !authorization.isEmpty()) {
                template.header("Authorization", authorization);
                System.out.println("Feign传递Authorization: " + authorization);
            }

            // 传递网关添加的用户信息头
            if (userId != null) {
                template.header("X-User-Id", userId.toString());
            }

            if (userName != null) {
                template.header("X-User-Name", userName);
            }

            if (userRole != null) {
                template.header("X-User-Role", userRole);
            }

            if (tokenSource != null) {
                template.header("X-Token-Source", tokenSource);
            }

            if (repackaged != null) {
                template.header("X-Token-Repackaged", repackaged.toString());
            }

            // 添加Feign调用的标识头
            template.header("X-Feign-Call", "true");
            template.header("X-Caller-Service", getServiceName());
        }

        private String getServiceName() {
            // 这里可以根据需要返回当前服务名
            // 可以从配置中读取，或者硬编码
            return System.getProperty("spring.application.name", "unknown-service");
        }
    }
}