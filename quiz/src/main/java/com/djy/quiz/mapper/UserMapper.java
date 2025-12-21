package com.djy.quiz.mapper;

import com.djy.quiz.pojo.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
  int insert(User user);

  int update(User user);

  int softDelete(@Param("userId") Long userId);

  User findById(@Param("userId") Long userId);

  User findByUserName(@Param("userName") String userName);

  List<User> listAll();
}
