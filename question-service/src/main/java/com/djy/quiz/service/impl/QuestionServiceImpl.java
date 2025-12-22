package com.djy.quiz.service.impl;

import com.djy.quiz.mapper.QuestionMapper;
import com.djy.quiz.pojo.model.Question;
import com.djy.quiz.service.QuestionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionServiceImpl implements QuestionService {

  private final QuestionMapper questionMapper;

  public QuestionServiceImpl(QuestionMapper questionMapper) {
    this.questionMapper = questionMapper;
  }

  @Override
  public void add(Question q) {
    questionMapper.insert(q);
  }

  @Override
  public void update(Question q) {
    questionMapper.update(q);
  }

  @Override
  public void delete(Integer questionId) {
    questionMapper.softDelete(questionId);
  }

  @Override
  public Question getById(Integer questionId) {
    Question question = questionMapper.findById(questionId);
    if (question == null) {
      throw new IllegalArgumentException("题目不存在");
    }
    return question;
  }

  @Override
  public List<Question> listAll() {
    return questionMapper.listAll();
  }
}
