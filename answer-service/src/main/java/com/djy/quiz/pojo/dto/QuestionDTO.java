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

  public QuestionDTO(Integer questionId,
                     String questionText,
                     String answer1Text, Integer answer1Correct,
                     String answer2Text, Integer answer2Correct,
                     String answer3Text, Integer answer3Correct,
                     String answer4Text, Integer answer4Correct) {
    this.questionId = questionId;
    this.questionText = questionText;
    this.answer1Text = answer1Text;
    this.answer1Correct = answer1Correct;
    this.answer2Text = answer2Text;
    this.answer2Correct = answer2Correct;
    this.answer3Text = answer3Text;
    this.answer3Correct = answer3Correct;
    this.answer4Text = answer4Text;
    this.answer4Correct = answer4Correct;
  }

  public QuestionDTO() {

  }
}
