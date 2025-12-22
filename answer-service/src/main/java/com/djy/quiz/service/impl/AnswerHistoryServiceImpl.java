package com.djy.quiz.service.impl;

import com.djy.quiz.mapper.AnswerHistoryMapper;
import com.djy.quiz.pojo.model.AnswerHistory;
import com.djy.quiz.service.AnswerHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnswerHistoryServiceImpl implements AnswerHistoryService {

  private final AnswerHistoryMapper answerHistoryMapper;

  public AnswerHistoryServiceImpl(AnswerHistoryMapper answerHistoryMapper) {
    this.answerHistoryMapper = answerHistoryMapper;
  }

  @Override
  public void add(AnswerHistory h) {
    answerHistoryMapper.insert(h);
  }

  @Override
  public void update(AnswerHistory h) {
    answerHistoryMapper.update(h);
  }

  @Override
  public void delete(Long answerHistoryId) {
    System.out.println("delete id："+answerHistoryId);
    answerHistoryMapper.softDelete(answerHistoryId);
  }

  @Override
  public AnswerHistory getById(Long answerHistoryId) {
    System.out.println("id："+answerHistoryId);
    return answerHistoryMapper.findById(answerHistoryId);
  }

  @Override
  public List<AnswerHistory> listByUser(Long userId) {
    return answerHistoryMapper.listByUser(userId);
  }

  @Override
  public List<AnswerHistory> listAll() {
    return answerHistoryMapper.listAll();
  }
}
