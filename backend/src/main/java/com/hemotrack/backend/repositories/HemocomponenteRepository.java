package com.hemotrack.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.StatusHemocomponente;

public interface HemocomponenteRepository extends JpaRepository<Hemocomponente, Long> {

    List<Hemocomponente> findByInstituicaoAtualIdAndStatus(Long instituicaoAtualId, StatusHemocomponente status);
}