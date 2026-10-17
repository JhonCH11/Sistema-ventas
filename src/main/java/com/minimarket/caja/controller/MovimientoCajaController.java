package com.minimarket.caja.controller;

import com.minimarket.caja.domain.EstadoCaja;
import com.minimarket.caja.form.*;
import com.minimarket.caja.service.*;
import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/caja/movimientos")
public class MovimientoCajaController {
    private final CajaService caja;
    private final MovimientoCajaService movimientos;
    public MovimientoCajaController(CajaService caja, MovimientoCajaService movimientos) { this.caja = caja; this.movimientos = movimientos; }
    @GetMapping
    public String mostrar(Model model) {
        preparar(model, new MovimientoCajaForm());
        return "caja/movimientos";
    }
    @PostMapping
    public String registrar(@Valid @ModelAttribute("formulario") MovimientoCajaForm form, BindingResult errores,
                            Authentication auth, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                movimientos.registrar(form, auth.getName());
                redirect.addFlashAttribute("mensaje", "Movimiento registrado. El efectivo esperado fue actualizado.");
                return "redirect:/caja/movimientos";
            } catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); }
        }
        preparar(model, form);
        return "caja/movimientos";
    }
    @PostMapping("/{id}/corregir")
    public String corregir(@PathVariable Long id, @Valid @ModelAttribute("correccion") CorreccionMovimientoCajaForm form,
                           BindingResult errores, Authentication auth, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                movimientos.corregir(id, form, auth.getName());
                redirect.addFlashAttribute("mensaje", "Correccion registrada. El movimiento original se conserva; registre el correcto si corresponde.");
                return "redirect:/caja/movimientos";
            } catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); }
        } else { model.addAttribute("error", "Indique un motivo de correccion de hasta 200 caracteres."); }
        model.addAttribute("correccionId", id);
        model.addAttribute("motivoCorreccion", form.getMotivo());
        preparar(model, new MovimientoCajaForm());
        return "caja/movimientos";
    }
    private void preparar(Model model, MovimientoCajaForm form) {
        if (form.getToken() == null) form.setToken(UUID.randomUUID().toString());
        model.addAttribute("formulario", form);
        var apertura = caja.buscarAperturaDeHoy();
        model.addAttribute("apertura", apertura.orElse(null));
        model.addAttribute("abierta", apertura.isPresent() && apertura.get().getEstado() == EstadoCaja.ABIERTA);
        apertura.ifPresent(a -> {
            if (form.getAperturaId() == null) form.setAperturaId(a.getId());
            var filas = movimientos.listar(a.getId());
            model.addAttribute("movimientos", filas);
            model.addAttribute("corregidos", filas.stream().filter(m -> m.getReversionDe() != null)
                    .map(m -> m.getReversionDe().getId()).collect(Collectors.toSet()));
            Map<Long, String> tokens = new HashMap<>();
            filas.forEach(m -> tokens.put(m.getId(), UUID.randomUUID().toString()));
            model.addAttribute("tokensCorreccion", tokens);
            model.addAttribute("efectivoEsperado", caja.calcularEfectivoEsperado(a));
        });
    }
}
