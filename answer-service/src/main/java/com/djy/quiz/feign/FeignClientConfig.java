package com.djy.quiz.feign;

import com.djy.quiz.util.JwtUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;
import java.util.Objects;

@Configuration
public class FeignClientConfig {

    private  final JwtUtil jwtUtil;

    public FeignClientConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new FeignRequestInterceptor();
    }

    public  class FeignRequestInterceptor implements RequestInterceptor {
        @Override
        public void apply(RequestTemplate template) {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (Objects.nonNull(attributes)) {
                HttpServletRequest request = attributes.getRequest();

                // 打印所有头部信息
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    String value = request.getHeader(name);
                    System.out.println("Header: " + name + " = " + value);
                }

                String token = request.getHeader("Authorization");
                token=jwtUtil.repackageToken(token);
                System.out.println("传递的Token: " + token);
                token = "Bearer " + token;
                System.out.println("完整的Authorization头: " + token);
                if (token != null && !token.isEmpty()) {
                    template.header("Authorization", token);
                    template.header("X-Token-Source", "thread-local");
                }
            }
        }
    }
}