package com.djy.quiz.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerSubmitDTO {
  @NotNull
  private Integer questionId;
  @NotNull
  @Min(1)
  @Max(4)
  private Integer selectedOption;
}
