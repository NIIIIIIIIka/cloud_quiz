package com.djy.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients // 启用 Feign 客户端
public class QuizApplication {
  public static void main(String[] args) {
    SpringApplication.run(QuizApplication.class, args);
  }
}
