package com.djy.quiz.mapper;

import com.djy.quiz.pojo.model.AnswerHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnswerHistoryMapper {
  int insert(AnswerHistory h);

  int update(AnswerHistory h);

  int softDelete(@Param("answerHistoryId") Long answerHistoryId);

  AnswerHistory findById( Long answerHistoryId);

  List<AnswerHistory> listByUser(@Param("userId") Long userId);

  List<AnswerHistory> listAll();
}
