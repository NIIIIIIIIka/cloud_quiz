package com.djy.quiz.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expireMinutes:120}")
  private long expireMinutes;

  private Key key;

  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
  }

  public String generateToken(Long userId, String userName, Integer role) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + expireMinutes * 60 * 1000);
    return Jwts.builder()
        .setSubject(String.valueOf(userId))
        .claim("userName", userName)
        .claim("role", role)
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
  /**
   * 重新包装Token，保留原有用户信息，更新签发时间和过期时间
   * @param originalToken 原始Token（可以带或不带"Bearer "前缀）
   * @return 重新包装后的Token
   */
  public String repackageToken(String originalToken) {
    try {
      // 去除可能的"Bearer "前缀
      String token = originalToken;
      if (originalToken != null && originalToken.startsWith("Bearer ")) {
        token = originalToken.substring(7);
      }

      // 解析原始Token的claims
      Claims claims = parseToken(token);

      // 获取原始用户信息
      String userId = claims.getSubject();
      String userName = claims.get("userName", String.class);
      Integer role = claims.get("role", Integer.class);


      // 生成新的Token，更新签发时间和过期时间
      Date now = new Date();
      Date exp = new Date(now.getTime() + 60 * 1000);

      return Jwts.builder()
              .setSubject(userId)
              .claim("userName", userName)
              .claim("role", role)
              .claim("repackaged", "true")
              .setIssuedAt(now)
              .setExpiration(exp)
              .signWith(key, SignatureAlgorithm.HS256)
              .compact();

    } catch (Exception e) {
      throw new RuntimeException("Token重新包装失败: " + e.getMessage(), e);
    }
  }
}
