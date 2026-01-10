package com.djy.quiz.pojo.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户事件消息 DTO
 */
@Data
public class UserEventDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 用户ID
   */
  private Long userId;

  /**
   * 用户名
   */
  private String userName;

  /**
   * 事件类型：REGISTER, LOGIN, LOGOUT
   */
  private String eventType;

  /**
   * 事件发生时间
   */
  private LocalDateTime eventTime;
}
