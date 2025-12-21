package com.djy.quiz.controller;

import com.djy.quiz.pojo.dto.UserLoginDTO;
import com.djy.quiz.pojo.dto.UserRegisterDTO;
import com.djy.quiz.pojo.vo.UserVO;
import com.djy.quiz.response.Result;
import com.djy.quiz.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * 用户注册
   */
  @PostMapping("/register")
  public Result<Void> register(@RequestBody @Valid UserRegisterDTO dto) {
    userService.register(dto);
    return Result.ok();
  }

  @GetMapping("/users/{id}")
  public Result<UserVO> getUser(@PathVariable("id") Long id) {
    return Result.ok(userService.getById(id));
  }


  /**
   * 用户登录，返回 token
   */
  @PostMapping("/login")
  public Result<Map<String, String>> login(@RequestBody @Valid UserLoginDTO dto) {
    String token = userService.login(dto);
    return Result.ok(Map.of("token", token));
  }

  /**
   * 获取当前用户信息
   */
  @GetMapping("/info")
  public Result<UserVO> info(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    return Result.ok(userService.getById(userId));
  }

  /**
   * 退出登录（前端删除token即可，此处仅返回成功）
   */
  @PostMapping("/logout")
  public Result<Void> logout() {
    return Result.ok();
  }
}
