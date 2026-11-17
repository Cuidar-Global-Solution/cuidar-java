package br.com.fiap.cuidar.handler;

import br.com.fiap.cuidar.dto.ErroResponse;
import br.com.fiap.cuidar.exception.ConflitoException;
import br.com.fiap.cuidar.exception.AreaCuidadoNaoEncontradaException;
import br.com.fiap.cuidar.exception.PessoaAcompanhadaNaoEncontradaException;
import br.com.fiap.cuidar.exception.PersistenciaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.HandlerMapping;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PersistenciaException.class)
    public ResponseEntity<Object> persistencia(PersistenciaException e, WebRequest request) {
        var servletRequest = ((ServletWebRequest) request).getRequest();
        Object rota = servletRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        Throwable causa = e.getCause();
        SQLException sqlException = null;
        Set<Throwable> visitadas = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable atual = causa; atual != null && visitadas.add(atual); atual = atual.getCause()) {
            if (atual instanceof SQLException sql) {
                sqlException = sql;
                break;
            }
        }
        String tipo = causa == null ? "ausente" : causa.getClass().getName();
        if (sqlException == null) {
            LOG.error("Falha de persistencia: metodo={} rota={} causa={}",
                    servletRequest.getMethod(), rota == null ? "desconhecida" : rota, tipo);
        } else {
            LOG.error("Falha de persistencia: metodo={} rota={} causa={} codigoFornecedor={} SQLState={}",
                    servletRequest.getMethod(), rota == null ? "desconhecida" : rota, tipo,
                    sqlException.getErrorCode(), sqlException.getSQLState());
        }
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, mensagem(HttpStatus.INTERNAL_SERVER_ERROR),
                List.of(), new HttpHeaders(), request);
    }
    @ExceptionHandler(br.com.fiap.cuidar.exception.AreaCuidadoNaoHabilitadaException.class)
    public ResponseEntity<Object> areaNaoHabilitada(
            br.com.fiap.cuidar.exception.AreaCuidadoNaoHabilitadaException e, WebRequest request) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), List.of(), new HttpHeaders(), request);
    }
    @ExceptionHandler(br.com.fiap.cuidar.exception.RegistroCuidadoNaoEncontradoException.class)
    public ResponseEntity<Object> registroNaoEncontrado(
            br.com.fiap.cuidar.exception.RegistroCuidadoNaoEncontradoException e, WebRequest request) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), List.of(), new HttpHeaders(), request);
    }
    @ExceptionHandler(AreaCuidadoNaoEncontradaException.class)
    public ResponseEntity<Object> areaNaoEncontrada(AreaCuidadoNaoEncontradaException e, WebRequest request) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), List.of(), new HttpHeaders(), request);
    }

    @ExceptionHandler(PessoaAcompanhadaNaoEncontradaException.class)
    public ResponseEntity<Object> naoEncontrada(PessoaAcompanhadaNaoEncontradaException e, WebRequest request) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), List.of(), new HttpHeaders(), request);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<Object> conflito(ConflitoException e, WebRequest request) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), List.of(), new HttpHeaders(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> inesperado(Exception e, WebRequest request) {
        // Preserva o status de erros HTTP do Spring; nunca publica a mensagem interna.
        HttpStatusCode status = e instanceof ErrorResponse error
                ? error.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR;
        HttpHeaders headers = e instanceof ErrorResponse error ? error.getHeaders() : new HttpHeaders();
        return resposta(status, mensagem(status), List.of(), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers,
                                                              HttpStatusCode status, WebRequest request) {
        List<String> detalhes = e instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage()).toList()
                : List.of();
        return resposta(status, mensagem(status), detalhes, headers, request);
    }

    private String mensagem(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Requisição inválida. Verifique os campos, o JSON e os parâmetros.";
            case 404 -> "Recurso não encontrado.";
            case 405 -> "Método HTTP não permitido para este recurso.";
            case 415 -> "Tipo de conteúdo não suportado.";
            default -> status.is5xxServerError() ? "Erro interno ao processar a requisição." : "Requisição não aceita.";
        };
    }

    private ResponseEntity<Object> resposta(HttpStatusCode status, String mensagem, List<String> detalhes,
                                             HttpHeaders headers, WebRequest request) {
        String caminho = ((ServletWebRequest) request).getRequest().getRequestURI();
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        var erro = new ErroResponse(Instant.now(), status.value(),
                httpStatus == null ? "Erro" : httpStatus.getReasonPhrase(), mensagem, caminho, detalhes);
        HttpHeaders respostaHeaders = new HttpHeaders();
        respostaHeaders.putAll(headers);
        respostaHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(erro, respostaHeaders, status);
    }
}
