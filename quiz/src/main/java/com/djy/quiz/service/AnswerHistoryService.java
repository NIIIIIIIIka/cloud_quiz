package com.djy.quiz.service;

import com.djy.quiz.pojo.model.AnswerHistory;
import java.util.List;

public interface AnswerHistoryService {
  void add(AnswerHistory h);

  void update(AnswerHistory h);

  void delete(Long answerHistoryId);

  AnswerHistory getById(Long answerHistoryId);

  List<AnswerHistory> listByUser(Long userId);

  List<AnswerHistory> listAll();
}
