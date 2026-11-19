package br.com.fiap.cuidar.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public record VinculoProfissional(long id, long pessoaAcompanhadaId, long profissionalId,
                                  String nomeProfissional, Permissao permissao, String autorizacao) {
    public enum Permissao {
        VISUALIZAR, REGISTRAR;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Permissao deJson(String valor) {
            return valueOf(valor);
        }
    }
}
