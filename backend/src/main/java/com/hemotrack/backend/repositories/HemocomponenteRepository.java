package com.hemotrack.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hemotrack.backend.model.sangue.Hemocomponente;

public interface HemocomponenteRepository extends JpaRepository<Hemocomponente, Long>{
    
}
