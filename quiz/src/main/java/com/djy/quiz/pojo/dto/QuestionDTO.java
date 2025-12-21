package com.djy.quiz.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionDTO {
  private Integer questionId;
  @NotBlank
  private String questionText;
  @NotBlank
  private String answer1Text;
  @NotNull
  private Integer answer1Correct;
  @NotBlank
  private String answer2Text;
  @NotNull
  private Integer answer2Correct;
  @NotBlank
  private String answer3Text;
  @NotNull
  private Integer answer3Correct;
  @NotBlank
  private String answer4Text;
  @NotNull
  private Integer answer4Correct;
}
