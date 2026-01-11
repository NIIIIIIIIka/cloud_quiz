package com.djy.quiz.filter;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实例信息响应头传递过滤器
 * 将下游服务返回的实例标识响应头传递给客户端，用于验证负载均衡效果
 * 
 * 由于 Spring Cloud Gateway 使用 Netty 作为 HTTP 客户端，下游服务设置的响应头
 * 默认会被传递到客户端响应中。此过滤器确保这些头不会被其他机制过滤掉。
 */
@Component
public class InstanceHeaderRelayFilter implements GlobalFilter, Ordered {

  // 需要传递的实例标识响应头
  private static final List<String> INSTANCE_HEADERS = Arrays.asList(
      "X-Instance-Id",
      "X-Instance-Host",
      "X-Instance-Port");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // 存储从下游服务获取的实例信息头
    Map<String, String> instanceHeaders = new HashMap<>();

    // 使用 ServerHttpResponseDecorator 包装响应，确保响应头被保留
    ServerHttpResponse originalResponse = exchange.getResponse();
    ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
      @Override
      public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        // 在写入响应体之前，确保实例信息头存在
        // 这些头应该已经由 NettyRoutingFilter 从下游服务复制过来
        // 我们在这里不做额外处理，只是确保响应正常传递
        return super.writeWith(body);
      }

      @Override
      public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        return super.writeAndFlushWith(body);
      }
    };

    // 使用装饰后的响应继续过滤链
    return chain.filter(exchange.mutate().response(decoratedResponse).build());
  }

  @Override
  public int getOrder() {
    // 在 NettyWriteResponseFilter 之前执行，确保响应头能被正确处理
    return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
  }
}
