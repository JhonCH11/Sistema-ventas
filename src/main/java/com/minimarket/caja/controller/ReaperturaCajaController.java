package com.minimarket.caja.controller;

import com.minimarket.caja.form.ReaperturaCajaForm;
import com.minimarket.caja.service.*;
import jakarta.validation.Valid;
import java.time.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/caja/reapertura")
public class ReaperturaCajaController {
    private final CajaService caja;
    private final ReaperturaCajaService service;
    public ReaperturaCajaController(CajaService caja, ReaperturaCajaService service) { this.caja = caja; this.service = service; }
    @GetMapping
    public String formulario(Model model, RedirectAttributes redirect) {
        var apertura = caja.buscarAperturaDeHoy();
        var cierre = apertura.flatMap(caja::buscarCierre);
        if (apertura.isEmpty() || apertura.get().getEstado() != com.minimarket.caja.domain.EstadoCaja.CERRADA || cierre.isEmpty()) {
            redirect.addFlashAttribute("errorGeneral", "Solo se puede reabrir una caja cerrada de hoy.");
            return "redirect:/caja";
        }
        var form = new ReaperturaCajaForm();
        form.setFecha(LocalDate.now(ZoneId.of("America/Lima")));
        form.setCierreId(cierre.get().getId());
        model.addAttribute("formulario", form);
        return "caja/reapertura";
    }
    @PostMapping
    public String reabrir(@Valid @ModelAttribute("formulario") ReaperturaCajaForm form, BindingResult resultado,
                          Authentication auth, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                service.reabrir(form, auth.getName());
                redirect.addFlashAttribute("mensaje", "Caja reabierta. Se conserva el cierre anterior. Puede extender el horario si lo necesita.");
                return "redirect:/caja";
            } catch (IllegalArgumentException ex) { model.addAttribute("errorGeneral", ex.getMessage()); }
        }
        return "caja/reapertura";
    }
}
