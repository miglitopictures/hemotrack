package com.hemotrack.backend.service;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hemotrack.backend.model.usuario.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service 
public class JwtService {

    public record TokenGerado(String valor, Instant expiraEm) {}

    private final SecretKey chave;
    private final long validadeHoras;

    public  JwtService(@Value("${hemotrack.jwt.segredo}") String segredo,
                       @Value("${hemotrack.jwt.validade-horas}") long validadeHoras) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.validadeHoras = validadeHoras;
    }

    public  TokenGerado gerar(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(validadeHoras, ChronoUnit.HOURS);

        String valor = Jwts.builder()
            .subject(String.valueOf(usuario.getId()))
            .claim("instituicaoId", usuario.getInstituicaoId())
            .claim("papel", usuario.getPapel().name())
            .issuedAt(Date.from(agora))
            .expiration(Date.from(expiraEm))
            .signWith(chave)
            .compact();

        return new  TokenGerado(valor, expiraEm);
    }

    public Claims validarEExtrair(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
