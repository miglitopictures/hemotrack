package com.hemotrack.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.repositories.RequisicaoRepository;



// codigo temporarios para testar pipeline de inegracao
@Service 
public class RequisicaoService {
    private final RequisicaoRepository repository;

    public RequisicaoService(RequisicaoRepository repository) {
        this.repository = repository;
    }

    public Requisicao salvar(Requisicao usuario) {
        return repository.save(usuario);
    }

    public Requisicao buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Requisicao> listarTodos() {
        return repository.findAll();
    }

    public  void remover(Long id) {
        repository.deleteById(id);
    }
}
