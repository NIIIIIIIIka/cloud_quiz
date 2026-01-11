package com.djy.quiz.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;

/**
 * 实例信息过滤器 - 在响应头中添加实例标识，用于验证负载均衡
 */
@Component
@Order(1)
public class InstanceInfoFilter implements Filter {

  @Value("${server.port:8083}")
  private String serverPort;

  @Value("${spring.application.name:answer-service}")
  private String applicationName;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // 添加实例标识响应头
    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      hostname = "unknown";
    }

    httpResponse.setHeader("X-Instance-Id", applicationName + ":" + serverPort);
    httpResponse.setHeader("X-Instance-Port", serverPort);
    httpResponse.setHeader("X-Instance-Host", hostname);

    chain.doFilter(request, response);
  }
}
