package com.djy.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class QuizApplication {
  public static void main(String[] args) {
    SpringApplication.run(QuizApplication.class, args);
  }
}
