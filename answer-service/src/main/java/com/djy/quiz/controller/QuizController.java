package com.djy.quiz.controller;

import com.djy.quiz.feign.QuestionServiceClient;
import com.djy.quiz.feign.UserServiceClient;
import com.djy.quiz.pojo.dto.AnswerSubmitDTO;
import com.djy.quiz.pojo.dto.QuestionDTO;
import com.djy.quiz.pojo.model.AnswerHistory;
import com.djy.quiz.pojo.vo.AnswerResultVO;
import com.djy.quiz.pojo.vo.AnswerHistoryVO;
import com.djy.quiz.pojo.vo.UserVO;
import com.djy.quiz.response.Result;
import com.djy.quiz.service.AnswerHistoryService;
import com.djy.quiz.util.Tools;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 答题相关接口 - 对应 /api/quiz 路径
 * 处理 submit 和 history 端点
 */
@Slf4j
@RestController
@RequestMapping("/api/quiz")
public class QuizController {
  private final Tools tools;
  private final QuestionServiceClient questionServiceClient;
  private final UserServiceClient userServiceClient;
  private final AnswerHistoryService answerHistoryService;

  public QuizController(Tools tools,
      @Qualifier("com.djy.quiz.feign.QuestionServiceClient") QuestionServiceClient questionServiceClient,
      @Qualifier("com.djy.quiz.feign.UserServiceClient") UserServiceClient userServiceClient,
      AnswerHistoryService answerHistoryService) {
    this.tools = tools;
    this.questionServiceClient = questionServiceClient;
    this.userServiceClient = userServiceClient;
    this.answerHistoryService = answerHistoryService;
  }

  /**
   * 提交答案
   * POST /api/quiz/submit
   * 使用 Seata 分布式事务保证跨服务数据一致性
   */
  @PostMapping("/submit")
  @GlobalTransactional(name = "submit-answer-tx", rollbackFor = Exception.class)
  public Result<AnswerResultVO> submit(@RequestBody @Valid AnswerSubmitDTO dto,
      HttpServletRequest request) {
    Long userId = Tools.getUserId();
    log.info("[Seata分布式事务] 用户提交答案开始: userId={}, questionId={}, selectedOption={}",
        userId, dto.getQuestionId(), dto.getSelectedOption());

    Result<UserVO> user = userServiceClient.getUserById(userId);
    Result<QuestionDTO> question = questionServiceClient.getQuestionById(dto.getQuestionId());

    if (user.isSuccess() && question.isSuccess()) {
      boolean correct = isCorrect(question.getData(), dto.getSelectedOption());

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
    } else if (!user.isSuccess()) {
      log.error("获取用户失败: code={}, message={}", user.getCode(), user.getMessage());
      return Result.error(user.getCode(), "获取用户失败: " + user.getMessage());
    } else {
      log.error("获取题目失败: code={}, message={}", question.getCode(), question.getMessage());
      return Result.error(question.getCode(), "获取题目失败: " + question.getMessage());
    }
  }

  /**
   * 获取当前用户答题记录
   * GET /api/quiz/history
   */
  @GetMapping("/history")
  public Result<List<AnswerHistoryVO>> getMyHistory(HttpServletRequest request) {
    Long userId = Tools.getUserId();
    log.info("获取用户答题记录: userId={}", userId);

    List<AnswerHistoryVO> list = answerHistoryService.listByUser(userId)
        .stream()
        .map(this::toHistoryVO)
        .collect(Collectors.toList());
    return Result.ok(list);
  }

  /* -------------------- 私有工具方法 -------------------- */
  private boolean isCorrect(QuestionDTO q, int option) {
    return switch (option) {
      case 1 -> q.getAnswer1Correct() == 1;
      case 2 -> q.getAnswer2Correct() == 1;
      case 3 -> q.getAnswer3Correct() == 1;
      case 4 -> q.getAnswer4Correct() == 1;
      default -> false;
    };
  }

  private AnswerHistoryVO toHistoryVO(AnswerHistory h) {
    AnswerHistoryVO vo = new AnswerHistoryVO();
    vo.setAnswerHistoryId(h.getAnswerHistoryId());
    vo.setUserId(h.getUserId());
    vo.setQuestionId(h.getQuestionId());
    vo.setSelectedOption(h.getSelectedOption());
    vo.setIsCorrect(h.getIsCorrect());
    vo.setAnswerTime(h.getAnswerTime() != null ? h.getAnswerTime().toString() : null);
    vo.setCreatedAt(h.getCreatedAt() != null ? h.getCreatedAt().toString() : null);
    vo.setUpdatedAt(h.getUpdatedAt() != null ? h.getUpdatedAt().toString() : null);
    return vo;
  }
}
