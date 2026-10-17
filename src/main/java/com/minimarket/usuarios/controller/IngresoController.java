package com.minimarket.usuarios.controller;

import com.minimarket.usuarios.service.UsuarioService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IngresoController {

    private final UsuarioService usuarioService;

    public IngresoController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/ingresar")
    public String mostrarFormulario(Authentication authentication) {
        if (!usuarioService.existeAlgunUsuario()) {
            return "redirect:/configuracion/inicial";
        }
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "usuarios/ingresar";
    }
}
