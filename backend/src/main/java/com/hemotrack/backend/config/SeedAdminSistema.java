package com.hemotrack.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.repositories.UsuarioRepository;

@Component 
public class SeedAdminSistema implements ApplicationRunner{
    

    private static final Logger log = LoggerFactory.getLogger(SeedAdminSistema.class);
    
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String nome;
    private final String email;
    private final String senha;

    // constructor
    public SeedAdminSistema(UsuarioRepository usuarios,
                            PasswordEncoder encoder,
                            @Value("${hemotrack.admin.nome}") String nome,
                            @Value("${hemotrack.admin.email}") String email,
                            @Value("${hemotrack.admin.senha}") String senha) {

        this.usuarios = usuarios;
        this.encoder = encoder;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override 
    public void run(ApplicationArguments argumentos) {


        if (email.isBlank() || senha.isBlank()) {
            log.warn("ADMIN_EMAIL/ADMIN_SENHA não configurados: nenhum ADMIN_SISTEMA foi criado. A aprovação de instituições ficará indisponível neste ambiente.");
            return;
        }

        if (usuarios.existsByPapel(Papel.ADMIN_SISTEMA)) {
            log.warn("ADMIN_SISTEMA já existe: seed ignorado.");
            return;
        }

        String emailNormalizado = email.trim().toLowerCase();

        if (usuarios.existsByEmail(emailNormalizado)) {
            log.warn("O email " + emailNormalizado + " já pertence a outro usuário; ADMIN_SISTEMA não foi criado.");
            return;
        }

        // passamos por todas as validacoes, podemos criar user admin do sistema
        usuarios.save(new Usuario(
            nome.trim(),
            emailNormalizado,
            encoder.encode(senha),
            Papel.ADMIN_SISTEMA,
            null));
        
        log.info("ADMIN_SISTEMA criado para " + emailNormalizado + ".");
    }
}
