package com.hemotrack.backend.model.usuario.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiraEm) { }
