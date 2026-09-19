package com.hemotrack.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.repositories.UsuarioRepository;



@Service 
public class UsuarioService {
    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public Usuario salvar(Usuario usuario) {
        return repository.save(usuario);
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public  void remover(Long id) {
        repository.deleteById(id);
    }
}
