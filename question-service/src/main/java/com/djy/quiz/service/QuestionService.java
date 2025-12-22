package com.djy.quiz.service;

import com.djy.quiz.pojo.model.Question;
import java.util.List;

public interface QuestionService {
  void add(Question q);

  void update(Question q);

  void delete(Integer questionId);

  Question getById(Integer questionId);

  List<Question> listAll();

}
