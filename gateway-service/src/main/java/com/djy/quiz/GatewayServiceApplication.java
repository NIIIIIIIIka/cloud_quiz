package com.djy.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@SpringBootApplication
@EnableDiscoveryClient // 启用 Nacos 服务发现
public class GatewayServiceApplication {
    public static void main(String[] args) {
//        System.out.println("这是最新的gateway");
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

}