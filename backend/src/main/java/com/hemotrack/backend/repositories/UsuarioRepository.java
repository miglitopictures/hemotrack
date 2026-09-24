package com.hemotrack.backend.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hemotrack.backend.model.usuario.Usuario;


// em todas essas classes que extendem JpaRepository, nos na temos que implementar "nada". A partir do nome do método (ex. findByEmail, findById), o Spring Data vai gerar o query SQL em tempo de execucao.
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{
    
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}