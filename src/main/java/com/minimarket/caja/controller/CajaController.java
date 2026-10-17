package com.minimarket.caja.controller;

import com.minimarket.caja.form.AperturaCajaForm;
import com.minimarket.caja.form.CierreCajaForm;
import com.minimarket.caja.service.CajaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/caja")
public class CajaController {

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE"));

    private final CajaService cajaService;
    private final com.minimarket.respaldos.service.RespaldoService respaldoService;

    public CajaController(CajaService cajaService,
                         com.minimarket.respaldos.service.RespaldoService respaldoService) {
        this.cajaService = cajaService;
        this.respaldoService = respaldoService;
    }

    @GetMapping
    public String mostrarCaja(Model model) {
        prepararPantalla(model, new AperturaCajaForm());
        return "caja/principal";
    }

    @ModelAttribute("administrador")
    public boolean esAdministrador(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    @PostMapping("/aperturas")
    public String abrirCaja(
            @Valid @ModelAttribute("formulario") AperturaCajaForm formulario,
            BindingResult resultado,
            Authentication autenticacion,
            Model model,
            RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            prepararPantalla(model, formulario);
            return "caja/principal";
        }
        try {
            cajaService.abrirCaja(formulario, autenticacion.getName());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorGeneral", ex.getMessage());
            prepararPantalla(model, formulario);
            return "caja/principal";
        }
        atributos.addFlashAttribute("mensaje", "Caja abierta correctamente.");
        return "redirect:/caja";
    }

    @PostMapping("/cierres")
    public String cerrarCaja(
            @Valid @ModelAttribute("cierreFormulario") CierreCajaForm formulario,
            BindingResult resultado,
            Authentication autenticacion,
            Model model,
            RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            model.addAttribute("cierreExpandido", true);
            prepararPantalla(model, new AperturaCajaForm());
            return "caja/principal";
        }
        try {
            cajaService.cerrarCaja(formulario, autenticacion.getName());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("cierreExpandido", true);
            model.addAttribute("errorGeneral", ex.getMessage());
            prepararPantalla(model, new AperturaCajaForm());
            return "caja/principal";
        }
        atributos.addFlashAttribute("mensaje", "Caja cerrada correctamente.");
        // cerrarCaja retorna despues del commit; un fallo del respaldo no revierte el cierre.
        try {
            atributos.addFlashAttribute("mensaje", "Caja cerrada. Respaldo guardado en: " + respaldoService.generar());
        } catch (IllegalStateException ex) {
            atributos.addFlashAttribute("errorGeneral", "La caja quedo cerrada, pero el respaldo fallo. " + ex.getMessage());
        }
        return "redirect:/caja";
    }

    private void prepararPantalla(Model model, AperturaCajaForm formulario) {
        if (!model.containsAttribute("formulario")) {
            model.addAttribute("formulario", formulario);
        }
        cajaService.buscarAperturaDeHoy().ifPresentOrElse(apertura -> {
            model.addAttribute("apertura", apertura);
            if (apertura.getEstado().name().equals("ABIERTA")) {
                model.addAttribute("efectivoEsperado", cajaService.calcularEfectivoEsperado(apertura));
            } else {
                model.addAttribute("cierre", cajaService.buscarCierre(apertura).orElse(null));
            }
        }, () -> model.addAttribute("apertura", null));
        if (!model.containsAttribute("cierreFormulario")) {
            model.addAttribute("cierreFormulario", new CierreCajaForm());
        }
        model.addAttribute("fechaActual", FORMATO_FECHA.format(LocalDate.now(ZONA_HORARIA)));
    }
}
