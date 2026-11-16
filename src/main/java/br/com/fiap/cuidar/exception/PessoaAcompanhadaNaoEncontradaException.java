package br.com.fiap.cuidar.exception;
public class PessoaAcompanhadaNaoEncontradaException extends RuntimeException {
    public PessoaAcompanhadaNaoEncontradaException() {
        super("Pessoa acompanhada não encontrada.");
    }
}
