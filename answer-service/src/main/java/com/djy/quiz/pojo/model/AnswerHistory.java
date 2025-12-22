package com.djy.quiz.pojo.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnswerHistory {
  private Long answerHistoryId;
  private Long userId;
  private Integer questionId;
  private Integer selectedOption; // 1-4
  private Integer isCorrect; // 0/1
  private LocalDateTime answerTime;
  private Integer isDeleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
