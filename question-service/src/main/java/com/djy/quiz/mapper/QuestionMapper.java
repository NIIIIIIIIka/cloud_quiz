package com.djy.quiz.mapper;

import com.djy.quiz.pojo.model.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper {
  int insert(Question q);

  int update(Question q);

  int softDelete(@Param("questionId") Integer questionId);

  Question findById(@Param("questionId") Integer questionId);

  List<Question> listAll();
}
