package com.djy.quiz.controller;

import com.djy.quiz.pojo.dto.AnswerSubmitDTO;
import com.djy.quiz.pojo.model.AnswerHistory;
import com.djy.quiz.pojo.model.Question;
import com.djy.quiz.pojo.vo.AnswerResultVO;
import com.djy.quiz.response.Result;
import com.djy.quiz.service.AnswerHistoryService;
import com.djy.quiz.service.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

  private final QuestionService questionService;
  private final AnswerHistoryService answerHistoryService;

  public QuizController(QuestionService questionService, AnswerHistoryService answerHistoryService) {
    this.questionService = questionService;
    this.answerHistoryService = answerHistoryService;
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

  /**
   * 提交答案
   */
  @PostMapping("/submit")
  public Result<AnswerResultVO> submit(@RequestBody @Valid AnswerSubmitDTO dto, HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    Question question = questionService.getById(dto.getQuestionId());
    boolean correct = isCorrect(question, dto.getSelectedOption());
    AnswerHistory h = new AnswerHistory();
    h.setUserId(userId);
    h.setQuestionId(dto.getQuestionId());
    h.setSelectedOption(dto.getSelectedOption());
    h.setIsCorrect(correct ? 1 : 0);
    h.setAnswerTime(LocalDateTime.now());
    answerHistoryService.add(h);

    AnswerResultVO vo = new AnswerResultVO();
    vo.setAnswerHistoryId(h.getAnswerHistoryId());
    vo.setQuestionId(dto.getQuestionId());
    vo.setSelectedOption(dto.getSelectedOption());
    vo.setIsCorrect(correct);
    vo.setAnswerTime(h.getAnswerTime());
    return Result.ok(vo);
  }

  /**
   * 获取当前用户答题记录
   */
  @GetMapping("/history")
  public Result<List<AnswerResultVO>> myHistory(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<AnswerResultVO> list = answerHistoryService.listByUser(userId).stream().map(this::toVO)
        .collect(Collectors.toList());
    return Result.ok(list);
  }

  private boolean isCorrect(Question q, int option) {
    return switch (option) {
      case 1 -> q.getAnswer1Correct() == 1;
      case 2 -> q.getAnswer2Correct() == 1;
      case 3 -> q.getAnswer3Correct() == 1;
      case 4 -> q.getAnswer4Correct() == 1;
      default -> false;
    };
  }

  private AnswerResultVO toVO(AnswerHistory h) {
    AnswerResultVO vo = new AnswerResultVO();
    vo.setAnswerHistoryId(h.getAnswerHistoryId());
    vo.setQuestionId(h.getQuestionId());
    vo.setSelectedOption(h.getSelectedOption());
    vo.setIsCorrect(h.getIsCorrect() == 1);
    vo.setAnswerTime(h.getAnswerTime());
    return vo;
  }
}
