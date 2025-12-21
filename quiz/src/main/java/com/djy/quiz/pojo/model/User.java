package com.djy.quiz.pojo.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
  private Long userId;
  private String userName;
  private String userPassword;
  private Integer userRole; // 0 user, 1 admin
  private Integer isDeleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
