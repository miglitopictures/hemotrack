package com.hemotrack.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hemotrack.backend.model.sangue.Bolsa;

public interface BolsaRepository extends JpaRepository<Bolsa, Long>{
   
}