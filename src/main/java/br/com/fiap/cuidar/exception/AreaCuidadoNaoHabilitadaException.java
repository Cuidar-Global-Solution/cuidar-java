package br.com.fiap.cuidar.exception;

public class AreaCuidadoNaoHabilitadaException extends RuntimeException {
    public AreaCuidadoNaoHabilitadaException() {
        super("A área de cuidado não está habilitada para esta pessoa acompanhada.");
    }
}
