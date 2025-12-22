package com.djy.quiz.controller;

import com.djy.quiz.feign.QuestionServiceClient;
import com.djy.quiz.feign.UserServiceClient;
import com.djy.quiz.pojo.dto.AnswerSubmitDTO;
import com.djy.quiz.pojo.dto.QuestionDTO;
import com.djy.quiz.pojo.model.AnswerHistory;
import com.djy.quiz.pojo.vo.AnswerResultVO;
import com.djy.quiz.pojo.vo.UserVO;
import com.djy.quiz.response.Result;
import com.djy.quiz.service.AnswerHistoryService;
import com.djy.quiz.util.Tools;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/answer")
public class AnswerController {
    private final Tools tools;
    private final QuestionServiceClient questionServiceClient;  // 使用Feign客户端
    private final UserServiceClient userServiceClient;          // 使用Feign客户端
    private final AnswerHistoryService answerHistoryService;

    public AnswerController(Tools tools,
                            @Qualifier("com.djy.quiz.feign.QuestionServiceClient")
                            QuestionServiceClient questionServiceClient,
                            @Qualifier("com.djy.quiz.feign.UserServiceClient")
                            UserServiceClient userServiceClient,
                            AnswerHistoryService answerHistoryService) {
        this.tools = tools;
        this.questionServiceClient = questionServiceClient;
        this.userServiceClient = userServiceClient;
        this.answerHistoryService = answerHistoryService;
    }

    /**
     * 提交答案
     */
    @PostMapping("/submit")
    public Result<AnswerResultVO> submit(@RequestBody @Valid AnswerSubmitDTO dto,
                                         HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");
//        Result<UserVO> user =userServiceClient.getUserById(userId);
        Result<QuestionDTO> question = questionServiceClient.getQuestionById(dto.getQuestionId());
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
        //        if(user.isSuccess()&&question.isSuccess()) {
//            boolean correct = isCorrect(question.getData(), dto.getSelectedOption());
//
//            AnswerHistory h = new AnswerHistory();
//            h.setUserId(userId);
//            h.setQuestionId(dto.getQuestionId());
//            h.setSelectedOption(dto.getSelectedOption());
//            h.setIsCorrect(correct ? 1 : 0);
//            h.setAnswerTime(LocalDateTime.now());
//            answerHistoryService.add(h);
//
//            AnswerResultVO vo = new AnswerResultVO();
//            vo.setAnswerHistoryId(h.getAnswerHistoryId());
//            vo.setQuestionId(dto.getQuestionId());
//            vo.setSelectedOption(dto.getSelectedOption());
//            vo.setIsCorrect(correct);
//            vo.setAnswerTime(h.getAnswerTime());
//            return Result.ok(vo);
//        } else if (user.isSuccess()) {
//            log.error("获取题目失败: code={}, message={}", question.getCode(), question.getMessage());
//            return Result.error(question.getCode(), "获取题目失败: " + question.getMessage());
//        }else {
//            log.error("获取用户失败: code={}, message={}", user.getCode(), user.getMessage());
//            return Result.error(user.getCode(), "获取题目失败: " + user.getMessage());
//        }
    }

    /**
     * 当前用户答题记录
     */
    @GetMapping("/history/user/{id}")
    public Result<List<AnswerResultVO>> HistoryByUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Result<UserVO> result = userServiceClient.getUserById(userId);
//        if (result.isSuccess()) {
            // 可以在这里添加answer-service特有的业务逻辑
            List<AnswerResultVO> list = answerHistoryService.listByUser(userId)
                    .stream()
                    .map(this::toVO)
                    .collect(Collectors.toList());
            return Result.ok(list);
//        }
//        return Result.error(404, "用户不存在");
    }
    @GetMapping("/history/all")
    public Result<List<AnswerHistory>> listHistory(HttpServletRequest request) {
        tools.checkAdmin(request);
        return Result.ok(answerHistoryService.listAll());
    }
    @GetMapping("/history/my")
    public Result<List<AnswerResultVO>> myHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<AnswerResultVO> list = answerHistoryService.listByUser(userId)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return Result.ok(list);
    }
    @GetMapping("/history/{id}")
    public Result<AnswerHistory> getHistoryById(@PathVariable("id") Long id, HttpServletRequest request) {
        tools.checkAdmin(request);
        AnswerHistory history=answerHistoryService.getById(id);
        if(history!=null){
            return Result.ok(history);}
        else {
            return  Result.error(404,"记录不存在");
        }

    }
    @DeleteMapping("/history/{id}")
    public Result<Void> deleteHistory(@PathVariable("id") Long id, HttpServletRequest request) {
        tools.checkAdmin(request);
        answerHistoryService.delete(id);
        return Result.ok();
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