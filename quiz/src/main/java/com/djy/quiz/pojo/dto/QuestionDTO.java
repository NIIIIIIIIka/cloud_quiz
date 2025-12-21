package com.djy.quiz.pojo.dto;

import com.djy.quiz.pojo.model.Question;
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
  public Question toModel(QuestionDTO dto) {
    Question q = new Question();
    q.setQuestionText(dto.getQuestionText());
    q.setAnswer1Text(dto.getAnswer1Text());
    q.setAnswer1Correct(dto.getAnswer1Correct());
    q.setAnswer2Text(dto.getAnswer2Text());
    q.setAnswer2Correct(dto.getAnswer2Correct());
    q.setAnswer3Text(dto.getAnswer3Text());
    q.setAnswer3Correct(dto.getAnswer3Correct());
    q.setAnswer4Text(dto.getAnswer4Text());
    q.setAnswer4Correct(dto.getAnswer4Correct());
    return q;
  }
}
