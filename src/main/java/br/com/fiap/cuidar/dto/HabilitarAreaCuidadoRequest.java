package br.com.fiap.cuidar.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HabilitarAreaCuidadoRequest(@NotNull @Positive @Max(999) Long areaId) {}
