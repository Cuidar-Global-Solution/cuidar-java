package br.com.fiap.cuidar.models;

import java.time.LocalDate;

public class PessoaIdosa {
    private Long id;

    private Long usuarioId;

    private String nome;

    private LocalDate dataNascimento;

    private String observacao;

    private Integer tamanhoFonte;

    private String altoContraste;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
