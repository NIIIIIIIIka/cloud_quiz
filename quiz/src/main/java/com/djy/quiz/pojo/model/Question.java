package com.djy.quiz.pojo.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Question {
  private Integer questionId;
  private String questionText;
  private String answer1Text;
  private Integer answer1Correct;
  private String answer2Text;
  private Integer answer2Correct;
  private String answer3Text;
  private Integer answer3Correct;
  private String answer4Text;
  private Integer answer4Correct;
  private Integer isDeleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
