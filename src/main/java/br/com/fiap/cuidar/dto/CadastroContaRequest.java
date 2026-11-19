package br.com.fiap.cuidar.dto;

import br.com.fiap.cuidar.models.Conta.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroContaRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 72) String senha,
        @NotNull Perfil perfil) {
    @Override
    public String toString() {
        return "CadastroContaRequest[perfil=" + perfil + ", senha=oculta]";
    }

    public CadastroContaRequest {
        if (nome != null) nome = nome.strip();
        if (email != null) email = email.strip();
    }
}
