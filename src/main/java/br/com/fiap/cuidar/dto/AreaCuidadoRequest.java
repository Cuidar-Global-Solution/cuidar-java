package br.com.fiap.cuidar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AreaCuidadoRequest {
    @NotBlank
    @Size(max = 50)
    private String nome;

    @Size(max = 200)
    private String descricao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
