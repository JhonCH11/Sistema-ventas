package com.minimarket.usuarios.controller;

import com.minimarket.usuarios.form.AdministradorInicialForm;
import com.minimarket.usuarios.service.UsuarioService;
import java.util.Objects;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/configuracion/inicial")
public class ConfiguracionInicialController {

    private final UsuarioService usuarioService;

    public ConfiguracionInicialController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String mostrarFormulario(Model model) {
        if (usuarioService.existeAlgunUsuario()) {
            return "redirect:/";
        }
        model.addAttribute("formulario", new AdministradorInicialForm());
        return "usuarios/configuracion-inicial";
    }

    @PostMapping
    public String registrar(
            @Valid @ModelAttribute("formulario") AdministradorInicialForm formulario,
            BindingResult resultado) {
        if (usuarioService.existeAlgunUsuario()) {
            return "redirect:/";
        }
        if (!Objects.equals(formulario.getContrasena(), formulario.getConfirmarContrasena())) {
            resultado.rejectValue("confirmarContrasena", "contrasena.distinta", "Las contraseñas no coinciden.");
        }
        if (resultado.hasErrors()) {
            return "usuarios/configuracion-inicial";
        }

        try {
            usuarioService.registrarAdministradorInicial(formulario);
        } catch (IllegalArgumentException ex) {
            resultado.rejectValue("nombreUsuario", "usuario.duplicado", ex.getMessage());
            return "usuarios/configuracion-inicial";
        }
        return "redirect:/";
    }
}
