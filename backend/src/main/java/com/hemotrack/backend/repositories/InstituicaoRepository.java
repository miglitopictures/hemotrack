package com.hemotrack.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hemotrack.backend.model.instituicao.Instituicao;

public interface InstituicaoRepository extends JpaRepository<Instituicao, Long>{
    boolean existsByCnpj(String cnpj);
    Optional<Instituicao> findByCnpj(String cnpj);
    
}
