package com.minimarket.caja.controller;

import com.minimarket.caja.form.ExtensionHorarioForm;
import com.minimarket.caja.service.HorarioCajaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/caja/horario")
public class HorarioCajaController {
    private final HorarioCajaService service;
    public HorarioCajaController(HorarioCajaService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<HorarioCajaService.Horario> consultar(Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultar(auth.getName()));
    }

    @GetMapping("/extender")
    public String formulario(Authentication auth, Model model) {
        var horario = service.consultar(auth.getName());
        var form = new ExtensionHorarioForm();
        form.setFecha(LocalDate.parse(horario.fecha()));
        model.addAttribute("extensionFormulario", form);
        model.addAttribute("horario", horario);
        return "caja/extender-horario";
    }

    @PostMapping("/extender")
    public String extender(@Valid @ModelAttribute("extensionFormulario") ExtensionHorarioForm form,
                           BindingResult resultado, Authentication auth, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                service.extender(form, auth.getName());
                redirect.addFlashAttribute("mensaje", "Horario de hoy extendido hasta las " + form.getNuevaHora() + ".");
                return "redirect:/caja";
            } catch (IllegalArgumentException ex) {
                model.addAttribute("errorGeneral", ex.getMessage());
            }
        }
        model.addAttribute("horario", service.consultar(auth.getName()));
        return "caja/extender-horario";
    }
}
