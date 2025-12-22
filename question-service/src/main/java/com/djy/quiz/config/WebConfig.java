package com.djy.quiz.config;

import com.djy.quiz.filter.JwtFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final JwtFilter jwtFilter;

  public WebConfig(JwtFilter jwtFilter) {
    this.jwtFilter = jwtFilter;
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

  @Bean
  public FilterRegistrationBean<JwtFilter> jwtFilterRegistration() {
    FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(jwtFilter);
    registration.addUrlPatterns("/api/*");
    registration.setOrder(1);
    return registration;
  }
}
