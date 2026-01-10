package com.djy.quiz.config;

//import com.djy.quiz.filter.JwtFilter;
import com.djy.quiz.util.JwtUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final JwtUtil jwtUtil;
  // @Bean // 改为通过 @Bean 创建 JwtFilter
  // public JwtFilter jwtFilter() {
  // return new JwtFilter(jwtUtil);
  // }

  public WebConfig(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(3600);
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
    stringConverter.setWriteAcceptCharset(false);
    converters.add(0, stringConverter);
  }

  // @Bean
  // public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter
  // jwtFilter) {
  // FilterRegistrationBean<JwtFilter> registration = new
  // FilterRegistrationBean<>();
  // registration.setFilter(jwtFilter);
  // registration.addUrlPatterns("/api/*");
  // registration.setOrder(1);
  // return registration;
  // }
}
