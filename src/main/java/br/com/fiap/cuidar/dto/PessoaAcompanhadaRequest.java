package br.com.fiap.cuidar.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public class PessoaAcompanhadaRequest {
    @Positive
    @Max(9999999999L)
    private Long usuarioId;

    @NotBlank
    @Size(max = 100)
    private String nome;

    @NotNull
    @PastOrPresent
    private LocalDate dataNascimento;

    @Size(max = 300)
    private String observacao;

    @NotNull
    @Min(12)
    @Max(32)
    private Integer tamanhoFonte;

    @NotNull
    @Pattern(regexp = "[SN]", message = "deve ser S ou N")
    private String altoContraste;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public Integer getTamanhoFonte() { return tamanhoFonte; }
    public void setTamanhoFonte(Integer tamanhoFonte) { this.tamanhoFonte = tamanhoFonte; }

    public String getAltoContraste() { return altoContraste; }
    public void setAltoContraste(String altoContraste) { this.altoContraste = altoContraste; }
}
