package com.djy.quiz.service;

import com.djy.quiz.pojo.dto.UserLoginDTO;
import com.djy.quiz.pojo.dto.UserRegisterDTO;
import com.djy.quiz.pojo.model.User;
import com.djy.quiz.pojo.vo.UserVO;

import java.util.List;

public interface UserService {
  void register(UserRegisterDTO dto);

  String login(UserLoginDTO dto);

  UserVO getById(Long userId);

  List<UserVO> listAll();

  void update(User user);

  void delete(Long userId);
}
