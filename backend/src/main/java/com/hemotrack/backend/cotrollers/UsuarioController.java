package com.hemotrack.backend.cotrollers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.service.UsuarioService;

import jakarta.validation.Valid;

@Controller 
@RequestMapping("/usuarios")
public class UsuarioController {
 
    private final UsuarioService service;

    // instância de UsuarioService é injetada pelo Spring no controller
    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // ponto de partida da aplicação (URL: /)
    //  Model: objeto usado para transportar dados do Controller para a View (HTML).
    @GetMapping({"", "/"})
    public String listar(Model model) {
       
	    // empacota a lista de clientes em um model, para que a view 
        // (arquivo HTML) possa acessar os dados.
        model.addAttribute("usuarios", service.listarTodos());
       

        // manda renderizar resources/templates/clientes.html
        // passando para esse html o model criado.
        return "usuarios";
    }



    @PostMapping("/salvar")
    public String salvar(@Valid Usuario usuario, BindingResult result) {
        if (result.hasErrors()) {
            return "usuario-editar";
        }
        service.salvar(usuario);
        return "redirect:/usuarios/";
    }
    
    // @PathVariable Long id --> Extrai o valor do ID da URL
    @GetMapping("/remover/{id}")
    public String remover(@PathVariable Long id) {
        service.remover(id);
        return "redirect:/usuarios/";
    }
    

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", service.buscarPorId(id));
        return "usuario-editar";
    }   

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "usuario-editar";
    }

}
