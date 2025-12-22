package com.djy.quiz.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegisterDTO {
  @NotBlank
  private String userName;
  @NotBlank
  private String password;
}
