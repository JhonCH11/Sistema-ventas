package com.minimarket.respaldos.controller;

import com.minimarket.respaldos.service.RespaldoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RespaldoController {
    private final RespaldoService service;

    public RespaldoController(RespaldoService service) { this.service = service; }

    @PostMapping("/configuracion/negocio/respaldo")
    public String generar(RedirectAttributes atributos) {
        try {
            atributos.addFlashAttribute("mensaje", "Respaldo guardado en: " + service.generar());
        } catch (IllegalStateException ex) {
            atributos.addFlashAttribute("errorRespaldo", ex.getMessage());
        }
        return "redirect:/configuracion/negocio";
    }
}
