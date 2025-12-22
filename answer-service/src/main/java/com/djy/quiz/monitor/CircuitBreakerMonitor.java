// CircuitBreakerMonitor.java
package com.djy.quiz.monitor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CircuitBreakerMonitor {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerMonitor(CircuitBreakerRegistry registry) {
        this.circuitBreakerRegistry = registry;
    }

    @Scheduled(fixedRate = 10000) // 每10秒监控一次
    public void monitorCircuitBreakers() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            CircuitBreaker.State state = circuitBreaker.getState();
            float failureRate = circuitBreaker.getMetrics().getFailureRate();

            log.info("熔断器[{}]状态: {}, 失败率: {}%, 请求总数: {}, 失败数: {}",
                    circuitBreaker.getName(),
                    state,
                    failureRate * 100,
                    circuitBreaker.getMetrics().getNumberOfBufferedCalls(),
                    circuitBreaker.getMetrics().getNumberOfFailedCalls()
            );
        });
    }
}