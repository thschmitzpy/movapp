package com.loja.movapp.dto;

import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequestDTO {

    @NotBlank(message = "Username é obrigatório")
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}