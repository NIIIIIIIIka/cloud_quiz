package com.djy.quiz.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.http.HttpStatus;
@Data
@NoArgsConstructor
//@AllArgsConstructor
public class Result<T> {
  private int code; // 200 成功，400 请求错误，404 找不到，500 服务器错误
  private String message;
  private T data;

  public static <T> Result<T> ok(T data) {
    return new Result<>(200, "success", data);
  }

  public static Result<Void> ok() {
    return new Result<>(200, "success", null);
  }

  public static Result<Void> error(String message) {
    return new Result<>(500, message, null); // 默认服务器错误
  }

  public static <T> Result<T> error(int code, String message) {
    return new Result<>(code, message, null);
  }

  // 常见快捷方法（可选）
  public static Result<Void> badRequest(String message) {
    return new Result<>(400, message, null);
  }

  public static Result<Void> notFound(String message) {
    return new Result<>(404, message, null);
  }

  public static Result<Void> forbidden(String message) {
    return new Result<>(403, message, null);
  }

  public static Result<Void> unauthorized(String message) {
    return new Result<>(401, message, null);
  }

  public static Result<Void> internalServerError(String message) {
    return new Result<>(500, message, null);
  }

  // 构造方法
  private Result(int code, String message, T data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  // Getters
  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }
}