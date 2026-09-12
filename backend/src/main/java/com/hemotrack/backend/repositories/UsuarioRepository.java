package com.hemotrack.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hemotrack.backend.model.usuario.Usuario;
// import org.springframework.stereotype.Repository;


//@Repository (inutil aparentemente, pois quando estamos usado JpaRepository)

public interface UsuarioRepository extends JpaRepository<Usuario, Long>{

    
}