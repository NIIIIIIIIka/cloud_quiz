package com.djy.quiz.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
  private int code; // 0 success, other error
  private String message;
  private T data;

  public static <T> Result<T> ok(T data) {
    return new Result<>(0, "success", data);
  }

  public static Result<Void> ok() {
    return new Result<>(0, "success", null);
  }

  public static Result<Void> error(String message) {
    return new Result<>(-1, message, null);
  }

  public static <T> Result<T> error(int code, String message) {
    return new Result<>(code, message, null);
  }
}
