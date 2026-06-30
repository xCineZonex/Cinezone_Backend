package com.cinezone.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO seguro para evaluar upgrade de nivel de fidelidad.
 * Solo recibe el ID del usuario, evitando mass assignment de la entidad User.
 */
public record EvaluateTierRequestDTO(
        @NotNull UUID userId
) {}
