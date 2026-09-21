package com.hemotrack.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hemotrack.backend.model.requisicao.Requisicao;

import java.util.List;

public interface RequisicaoRepository extends JpaRepository<Requisicao, Long>{

    List<Requisicao> findByHospitalId(Long hospitalId);

}
