package br.com.fiap.cuidar.dto;

import java.time.Instant;
import java.util.List;

public record ErroResponse(Instant timestamp, int status, String erro, String mensagem,
                           String caminho, List<String> detalhes) { }
