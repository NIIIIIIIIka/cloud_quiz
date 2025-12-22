package com.djy.quiz.service.impl;

import com.djy.quiz.constant.RoleConstant;
import com.djy.quiz.mapper.UserMapper;
import com.djy.quiz.pojo.dto.UserLoginDTO;
import com.djy.quiz.pojo.dto.UserRegisterDTO;
import com.djy.quiz.pojo.model.User;
import com.djy.quiz.pojo.vo.UserVO;
import com.djy.quiz.service.UserService;
import com.djy.quiz.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public UserServiceImpl(UserMapper userMapper, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }

  @Override
  public void register(UserRegisterDTO dto) {
    if (userMapper.findByUserName(dto.getUserName()) != null) {
      throw new IllegalArgumentException("用户名已存在");
    }
    User user = new User();
    user.setUserName(dto.getUserName());
    user.setUserPassword(passwordEncoder.encode(dto.getPassword()));
    user.setUserRole(RoleConstant.USER);
    userMapper.insert(user);
  }

  @Override
  public String login(UserLoginDTO dto) {
    User user = userMapper.findByUserName(dto.getUserName());
    if (user == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    if (!passwordEncoder.matches(dto.getPassword(), user.getUserPassword())) {
      throw new IllegalArgumentException("密码错误");
    }
    return jwtUtil.generateToken(user.getUserId(), user.getUserName(), user.getUserRole());
  }

  @Override
  public UserVO getById(Long userId) {
    User user = userMapper.findById(userId);
    if (user == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    return toVO(user);
  }

  @Override
  public List<UserVO> listAll() {
    return userMapper.listAll().stream().map(this::toVO).collect(Collectors.toList());
  }

  @Override
  public void update(User user) {
    if (user.getUserPassword() != null && !user.getUserPassword().isBlank()) {
      user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
    }
    userMapper.update(user);
  }

  @Override
  public void delete(Long userId) {
    userMapper.softDelete(userId);
  }

  private UserVO toVO(User u) {
    UserVO vo = new UserVO();
    vo.setUserId(u.getUserId());
    vo.setUserName(u.getUserName());
    vo.setUserRole(u.getUserRole());
    return vo;
  }
}
