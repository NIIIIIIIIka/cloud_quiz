package com.djy.quiz.pojo.vo;

import lombok.Data;

@Data
public class AnswerHistoryVO {
  private Long answerHistoryId;
  private Long userId;
  private Integer questionId;
  private Integer selectedOption; // 1-4
  private Integer isCorrect; // 0/1
  private String answerTime;
  private String createdAt;
  private String updatedAt;
}