package br.com.fiap.cuidar.exception;
public class PessoaIdosaNaoEncontradaException extends RuntimeException {
    public PessoaIdosaNaoEncontradaException() {
        super("Pessoa idosa não encontrada.");
    }
}
