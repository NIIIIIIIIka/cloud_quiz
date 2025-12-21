package com.djy.quiz.controller;

import com.djy.quiz.pojo.dto.QuestionDTO;
import com.djy.quiz.pojo.model.Question;
import com.djy.quiz.response.Result;
import com.djy.quiz.service.QuestionService;
import com.djy.quiz.util.Tools;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {
  private final Tools tools;
  private final QuestionService questionService;

  public QuizController(Tools tools, QuestionService questionService) {
    this.tools = tools;
    this.questionService = questionService;
  }

  /**
   * 获取题目列表
   */
  @GetMapping("/questions")
  public Result<List<Question>> listQuestions() {
    return Result.ok(questionService.listAll());
  }

  /**
   * 获取单个题目
   */
  @GetMapping("/questions/{id}")
  public Result<Question> getQuestion(@PathVariable("id") Integer id) {
    return Result.ok(questionService.getById(id));
  }

  @PostMapping("/questions")
  public Result<Void> addQuestion(@RequestBody @Valid QuestionDTO dto, HttpServletRequest request) {
    tools.checkAdmin(request);
    Question q = dto.toModel(dto);
    questionService.add(q);
    return Result.ok();
  }

  @PutMapping("/questions/{id}")
  public Result<Void> updateQuestion(@PathVariable("id") Integer id, @RequestBody @Valid QuestionDTO dto,
                                     HttpServletRequest request) {
    tools.checkAdmin(request);
    Question q = dto.toModel(dto);
    q.setQuestionId(id);
    questionService.update(q);
    return Result.ok();
  }

  @DeleteMapping("/questions/{id}")
  public Result<Void> deleteQuestion(@PathVariable("id") Integer id, HttpServletRequest request) {
    tools.checkAdmin(request);
    questionService.delete(id);
    return Result.ok();
  }
}