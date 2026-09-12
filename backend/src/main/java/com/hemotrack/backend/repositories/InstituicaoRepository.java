package com.hemotrack.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hemotrack.backend.model.instituicao.Instituicao;

public interface InstituicaoRepository extends JpaRepository<Instituicao, Long>{
    
}
