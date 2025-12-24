package com.djy.quiz.filter;

import com.djy.quiz.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtGatewayFilter extends AbstractGatewayFilterFactory<JwtGatewayFilter.Config> implements Ordered {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 网关级别的白名单（不需要token的路径）
    private static final List<String> GATEWAY_WHITE_LIST = Arrays.asList(
            "/api/user/register",
            "/api/user/login",
            "/actuator/health",
            "/actuator/info",
            "/health"
    );

    public JwtGatewayFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    public static class Config {
        // 可以通过配置文件传递额外的白名单
        private String whiteList;

        public String getWhiteList() {
            return whiteList;
        }

        public void setWhiteList(String whiteList) {
            this.whiteList = whiteList;
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            String method = request.getMethod().name();

            // 放行OPTIONS请求（CORS预检）
            if ("OPTIONS".equalsIgnoreCase(method)) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.OK);
                return response.setComplete();
            }

            // 检查是否在白名单中
            if (isWhiteListed(path, GATEWAY_WHITE_LIST)) {
                // 合并配置文件中的白名单
                if (config.getWhiteList() != null) {
                    List<String> additionalWhiteList = Arrays.asList(config.getWhiteList().split(","));
                    if (isWhiteListed(path, additionalWhiteList)) {
                        return chain.filter(exchange);
                    }
                }
                return chain.filter(exchange);
            }

            // 获取Authorization头
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "missing or invalid token");
            }

            String token = authHeader.substring(7);

            // 获取X-Token-Source头（用于特殊token处理）
            String tokenSource = request.getHeaders().getFirst("X-Token-Source");

            try {
                // 解析JWT token
                Claims claims = jwtUtil.parseToken(token);

                // 构建新的请求，添加用户信息到header中
                ServerHttpRequest.Builder requestBuilder = request.mutate();

                // 添加用户信息到请求头（微服务可以通过这些头获取用户信息）
                requestBuilder.header("X-User-Id", claims.getSubject());

                Object userName = claims.get("userName");
                if (userName != null) {
                    requestBuilder.header("X-User-Name", userName.toString());
                }

                Object role = claims.get("role");
                if (role != null) {
                    requestBuilder.header("X-User-Role", role.toString());
                }

                // 处理X-Token-Source和repackaged逻辑
                if (StringUtils.hasText(tokenSource)) {
                    System.out.println("Token来源: " + tokenSource);
                    requestBuilder.header("X-Token-Source", tokenSource);
                    System.out.println("已设置 X-Token-Source 请求头: " + tokenSource);

                    Object repackaged = claims.get("repackaged");
                    if (repackaged != null) {
                        System.out.println("claims.get(\"repackaged\"): " + repackaged);
                        requestBuilder.header("X-Token-Repackaged", repackaged.toString());
                        System.out.println("已设置 X-Token-Repackaged 请求头: " + repackaged.toString());

                        // 设置到attribute中（如果需要）
                        exchange.getAttributes().put("repackaged", repackaged);
                        System.out.println("已将 repackaged 存入 exchange attributes: " + repackaged);
                    }
                }

// 保留原始token，以便微服务可能还需要使用
                requestBuilder.header("Authorization", authHeader);
                System.out.println("已设置 Authorization 请求头: " + (authHeader != null ? "有值" : "空值"));

// 将claims中的其他信息也传递下去（可选）
                System.out.println("开始处理其他claims信息...");
                int claimCount = 0;
                for (Map.Entry<String, Object> entry : claims.entrySet()) {
                    if (!entry.getKey().equals("sub") &&
                            !entry.getKey().equals("userName") &&
                            !entry.getKey().equals("role") &&
                            !entry.getKey().equals("repackaged")) {

                        String headerName = "X-Claim-" + entry.getKey();
                        String headerValue = entry.getValue() != null ? entry.getValue().toString() : "";

                        requestBuilder.header(headerName, headerValue);
                        System.out.println("已设置请求头 [" + headerName + "]: " +
                                (headerValue.length() > 50 ? headerValue.substring(0, 50) + "..." : headerValue));
                        claimCount++;
                    }
                }
                System.out.println("共设置了 " + claimCount + " 个额外的claim请求头");

// 可选：打印所有已设置的请求头（调试用）
                System.out.println("--- 请求头设置完成 ---");

                // 构建修改后的请求
                ServerHttpRequest mutatedRequest = requestBuilder.build();

                // 将用户信息保存到exchange的attribute中，供后续过滤器或处理器使用
                exchange.getAttributes().put("userId", Long.valueOf(claims.getSubject()));
                exchange.getAttributes().put("userName", userName);
                exchange.getAttributes().put("role", role);

                // 继续过滤器链
                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                System.out.println("Token已过期: " + e.getMessage());
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "token expired");
            } catch (io.jsonwebtoken.MalformedJwtException e) {
                System.out.println("Token格式错误: " + e.getMessage());
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "malformed token");
            } catch (io.jsonwebtoken.security.SignatureException e) {
                System.out.println("Token签名无效: " + e.getMessage());
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "invalid token signature");
            } catch (Exception e) {
                System.out.println("Token验证失败: " + e.getClass().getName() + " - " + e.getMessage());
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "token invalid or expired");
            }
        };
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteListed(String path, List<String> whiteList) {
        if (whiteList == null || whiteList.isEmpty()) {
            return false;
        }
        return whiteList.stream().anyMatch(whitePath -> {
            // 支持精确匹配和前缀匹配
            return path.equals(whitePath) || path.startsWith(whitePath);
        });
    }

    /**
     * 写入错误响应
     */
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Charset", "UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", -1);
        result.put("message", message);
        result.put("data", null);
        result.put("success", false);
        result.put("timestamp", System.currentTimeMillis());

        try {
            byte[] bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            // 如果JSON序列化失败，返回简单的错误信息
            byte[] bytes = ("{\"code\":-1,\"message\":\"Internal server error\",\"success\":false}")
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }

    /**
     * 设置过滤器执行顺序（数字越小优先级越高）
     */
    @Override
    public int getOrder() {
        return -100; // 高优先级，确保在其他过滤器之前执行
    }

    /**
     * 快捷方式，用于直接创建过滤器实例
     */
    public GatewayFilter apply() {
        return apply(new Config());
    }
}