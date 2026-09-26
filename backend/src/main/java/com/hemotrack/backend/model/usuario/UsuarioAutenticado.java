package com.hemotrack.backend.model.usuario;

// quem esta fazendo a requisicao (JWT).
public record UsuarioAutenticado(Long usuarioId, Long instituicaoId, Papel papel) { }
