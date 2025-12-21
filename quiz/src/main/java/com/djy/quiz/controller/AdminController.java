package com.djy.quiz.controller;

import com.djy.quiz.constant.RoleConstant;
import com.djy.quiz.pojo.dto.QuestionDTO;
import com.djy.quiz.pojo.model.AnswerHistory;
import com.djy.quiz.pojo.model.Question;
import com.djy.quiz.pojo.model.User;
import com.djy.quiz.pojo.vo.UserVO;
import com.djy.quiz.response.Result;
import com.djy.quiz.service.AnswerHistoryService;
import com.djy.quiz.service.QuestionService;
import com.djy.quiz.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final UserService userService;
  private final QuestionService questionService;
  private final AnswerHistoryService answerHistoryService;

  public AdminController(UserService userService, QuestionService questionService,
      AnswerHistoryService answerHistoryService) {
    this.userService = userService;
    this.questionService = questionService;
    this.answerHistoryService = answerHistoryService;
  }

  /**
   * 管理员鉴权校验（抛出异常由全局处理器返回错误）
   */
  private void checkAdmin(HttpServletRequest request) {
    Integer role = (Integer) request.getAttribute("role");
    if (role == null || role != RoleConstant.ADMIN) {
      throw new IllegalArgumentException("无管理员权限");
    }
  }

  // ========== 用户管理 ==========
  @GetMapping("/users")
  public Result<List<UserVO>> listUsers(HttpServletRequest request) {
    checkAdmin(request);
    return Result.ok(userService.listAll());
  }

  @GetMapping("/users/{id}")
  public Result<UserVO> getUser(@PathVariable("id") Long id, HttpServletRequest request) {
    checkAdmin(request);
    return Result.ok(userService.getById(id));
  }

  @PutMapping("/users/{id}")
  public Result<Void> updateUser(@PathVariable("id") Long id, @RequestBody User user, HttpServletRequest request) {
    checkAdmin(request);
    user.setUserId(id);
    userService.update(user);
    return Result.ok();
  }

  @DeleteMapping("/users/{id}")
  public Result<Void> deleteUser(@PathVariable("id") Long id, HttpServletRequest request) {
    checkAdmin(request);
    userService.delete(id);
    return Result.ok();
  }

  // ========== 题目管理 ==========
  @GetMapping("/questions")
  public Result<List<Question>> listQuestions(HttpServletRequest request) {
    checkAdmin(request);
    return Result.ok(questionService.listAll());
  }

  @GetMapping("/questions/{id}")
  public Result<Question> getQuestion(@PathVariable("id") Integer id, HttpServletRequest request) {
    checkAdmin(request);
    return Result.ok(questionService.getById(id));
  }

  @PostMapping("/questions")
  public Result<Void> addQuestion(@RequestBody @Valid QuestionDTO dto, HttpServletRequest request) {
    checkAdmin(request);
    Question q = toModel(dto);
    questionService.add(q);
    return Result.ok();
  }

  @PutMapping("/questions/{id}")
  public Result<Void> updateQuestion(@PathVariable("id") Integer id, @RequestBody @Valid QuestionDTO dto,
      HttpServletRequest request) {
    checkAdmin(request);
    Question q = toModel(dto);
    q.setQuestionId(id);
    questionService.update(q);
    return Result.ok();
  }

  @DeleteMapping("/questions/{id}")
  public Result<Void> deleteQuestion(@PathVariable("id") Integer id, HttpServletRequest request) {
    checkAdmin(request);
    questionService.delete(id);
    return Result.ok();
  }

  // ========== 答题记录管理 ==========
  @GetMapping("/history")
  public Result<List<AnswerHistory>> listHistory(HttpServletRequest request) {
    checkAdmin(request);
    return Result.ok(answerHistoryService.listAll());
  }

  @GetMapping("/history/{id}")
  public Result<AnswerHistory> getHistory(@PathVariable("id") Long id, HttpServletRequest request) {
    checkAdmin(request);
    return Result.ok(answerHistoryService.getById(id));
  }

  @DeleteMapping("/history/{id}")
  public Result<Void> deleteHistory(@PathVariable("id") Long id, HttpServletRequest request) {
    checkAdmin(request);
    answerHistoryService.delete(id);
    return Result.ok();
  }

  private Question toModel(QuestionDTO dto) {
    Question q = new Question();
    q.setQuestionText(dto.getQuestionText());
    q.setAnswer1Text(dto.getAnswer1Text());
    q.setAnswer1Correct(dto.getAnswer1Correct());
    q.setAnswer2Text(dto.getAnswer2Text());
    q.setAnswer2Correct(dto.getAnswer2Correct());
    q.setAnswer3Text(dto.getAnswer3Text());
    q.setAnswer3Correct(dto.getAnswer3Correct());
    q.setAnswer4Text(dto.getAnswer4Text());
    q.setAnswer4Correct(dto.getAnswer4Correct());
    return q;
  }
}
