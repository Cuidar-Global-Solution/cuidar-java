package br.com.fiap.cuidar.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public record Conta(long id, String nome, String email, Perfil perfil) {
    public enum Perfil {
        FAMILIAR, PROFISSIONAL, ACOMPANHADO;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Perfil deJson(String valor) {
            return valueOf(valor);
        }
    }
}
