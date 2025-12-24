//package com.djy.quiz.filter;
//
//import com.djy.quiz.response.Result;
//import com.djy.quiz.util.JwtUtil;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.jsonwebtoken.Claims;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//
////@Component
//public class JwtFilter implements Filter {
//
//  private final JwtUtil jwtUtil;
//  private final ObjectMapper objectMapper = new ObjectMapper();
//
//  // 放行路径 (无需token)
//  private static final List<String> WHITE_LIST = Arrays.asList(
//      "/api/user/register",
//      "/api/user/login"
//  );
//
//  public JwtFilter(JwtUtil jwtUtil) {
//    this.jwtUtil = jwtUtil;
//  }
//
//  @Override
//  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
//      throws IOException, ServletException {
//    HttpServletRequest request = (HttpServletRequest) servletRequest;
//    HttpServletResponse response = (HttpServletResponse) servletResponse;
//
//    String path = request.getRequestURI();
//    // 放行白名单
//    if (isWhiteListed(path)) {
//      chain.doFilter(request, response);
//      return;
//    }
//
//    // 放行 CORS 预检
//    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
//      response.setStatus(HttpServletResponse.SC_OK);
//      return;
//    }
//
//    String authHeader = request.getHeader("Authorization");
//    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//      writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "missing or invalid token");
//      return;
//    }
//
//    String token = authHeader.substring(7);
//    try {
//      Claims claims = jwtUtil.parseToken(token);
//      // 挂载到 request 供后续使用
//      request.setAttribute("userId", Long.valueOf(claims.getSubject()));
//      request.setAttribute("userName", claims.get("userName"));
//      request.setAttribute("role", claims.get("role"));
//      chain.doFilter(request, response);
//    } catch (Exception e) {
//      writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "token invalid or expired");
//    }
//  }
//
//  private boolean isWhiteListed(String path) {
//    for (String w : WHITE_LIST) {
//      if (path.startsWith(w)) {
//        return true;
//      }
//    }
//    return false;
//  }
//
//  private void writeError(HttpServletResponse response, int status, String message) throws IOException {
//    response.setStatus(status);
//    response.setContentType("application/json;charset=UTF-8");
//    Result<Void> result = Result.error(-1, message);
//    response.getWriter().write(objectMapper.writeValueAsString(result));
//  }
//}
