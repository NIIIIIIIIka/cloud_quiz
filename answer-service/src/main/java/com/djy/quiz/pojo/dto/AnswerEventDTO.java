package com.djy.quiz.pojo.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 答题事件消息 DTO
 */
@Data
public class AnswerEventDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long userId;
  private Long questionId;
  private Boolean isCorrect;
  private Integer score;
  private LocalDateTime eventTime;
}
