package com.cinezone.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DniSearchRequestDTO(
        @NotBlank @Size(min = 8, max = 15) String dni
) {}