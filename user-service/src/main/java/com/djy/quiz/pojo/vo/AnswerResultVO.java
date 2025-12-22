package com.djy.quiz.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnswerResultVO {
  private Long answerHistoryId;
  private Integer questionId;
  private Integer selectedOption;
  private Boolean isCorrect;
  private LocalDateTime answerTime;
}
