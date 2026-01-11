package com.djy.quiz.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 在响应头中暴露上游实例信息（主机、端口、路由ID），
 * 方便客户端脚本统计负载均衡分布。
 */
@Component
public class UpstreamInstanceExposeFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return chain.filter(exchange).then(Mono.fromRunnable(() -> {
      ServerHttpResponse response = exchange.getResponse();

      URI upstream = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
      if (upstream != null) {
        if (!response.getHeaders().containsKey("X-Instance-Host") && upstream.getHost() != null) {
          response.getHeaders().set("X-Instance-Host", upstream.getHost());
        }
        if (!response.getHeaders().containsKey("X-Instance-Port") && upstream.getPort() > 0) {
          response.getHeaders().set("X-Instance-Port", String.valueOf(upstream.getPort()));
        }
      }

      Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
      if (route != null && !response.getHeaders().containsKey("X-Instance-Id")) {
        response.getHeaders().set("X-Instance-Id", route.getId());
      }
    }));
  }

  @Override
  public int getOrder() {
    // 在写出响应前设置响应头
    return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
  }
}
