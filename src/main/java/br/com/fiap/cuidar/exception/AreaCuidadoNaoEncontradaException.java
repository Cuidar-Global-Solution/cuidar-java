package br.com.fiap.cuidar.exception;

public class AreaCuidadoNaoEncontradaException extends RuntimeException {
    public AreaCuidadoNaoEncontradaException() {
        super("Área de cuidado não encontrada.");
    }
}
