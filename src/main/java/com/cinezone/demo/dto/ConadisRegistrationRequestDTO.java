package com.cinezone.demo.dto;

import java.util.UUID;

public record ConadisRegistrationRequestDTO(
    UUID boletaId,
    String ruid,
    String dni
) {}
