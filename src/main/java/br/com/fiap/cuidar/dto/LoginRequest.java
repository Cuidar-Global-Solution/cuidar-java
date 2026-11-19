package br.com.fiap.cuidar.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Email @Size(max = 120) String email,
                           @NotBlank @Size(max = 72) String senha) {
    @Override
    public String toString() {
        return "LoginRequest[senha=oculta]";
    }

    public LoginRequest {
        if (email != null) email = email.strip();
    }
}
