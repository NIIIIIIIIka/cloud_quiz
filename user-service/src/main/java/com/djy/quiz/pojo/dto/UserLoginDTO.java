package com.djy.quiz.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDTO {
  @NotBlank
  private String userName;
  @NotBlank
  private String password;
}
