package com.minimarket.configuracion.controller;

import com.minimarket.configuracion.form.ConfiguracionNegocioForm;
import com.minimarket.configuracion.service.ConfiguracionNegocioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracion/negocio")
public class ConfiguracionNegocioController {
    private final ConfiguracionNegocioService service;

    public ConfiguracionNegocioController(ConfiguracionNegocioService service) {
        this.service = service;
    }

    @GetMapping
    public String mostrar(Model model) {
        var negocio = service.obtener();
        var formulario = new ConfiguracionNegocioForm();
        formulario.setNombre(negocio.getNombre());
        formulario.setDireccion(negocio.getDireccion());
        formulario.setTelefono(negocio.getTelefono());
        formulario.setRutaRespaldos(negocio.getRutaRespaldos());
        model.addAttribute("formulario", formulario);
        return "configuracion/negocio";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("formulario") ConfiguracionNegocioForm formulario,
                          BindingResult resultado, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return "configuracion/negocio";
        }
        try {
            service.guardar(formulario);
        } catch (IllegalArgumentException ex) {
            resultado.rejectValue("rutaRespaldos", "ruta.invalida", ex.getMessage());
            return "configuracion/negocio";
        }
        redirect.addFlashAttribute("mensaje", "Datos del minimarket guardados.");
        return "redirect:/configuracion/negocio";
    }
}
