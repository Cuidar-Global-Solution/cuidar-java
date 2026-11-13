package br.com.fiap.cuidar.exception;
public class PersistenciaException extends RuntimeException {
    public PersistenciaException(Throwable causa) {
        super("Não foi possível acessar os dados.", causa);
    }
}
