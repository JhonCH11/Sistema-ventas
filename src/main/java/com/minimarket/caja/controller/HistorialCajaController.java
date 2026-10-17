package com.minimarket.caja.controller;

import com.minimarket.caja.service.HistorialCajaService;
import java.time.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/caja/historial")
public class HistorialCajaController {
    private final HistorialCajaService historial;
    public HistorialCajaController(HistorialCajaService historial) { this.historial=historial; }
    @GetMapping
    public String listar(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,Model model) {
        var rango=fechas(desde,hasta);model.addAttribute("desde",rango[0]);model.addAttribute("hasta",rango[1]);
        try { model.addAttribute("jornadas",historial.listar(rango[0],rango[1])); }
        catch(IllegalArgumentException ex) { model.addAttribute("error",ex.getMessage()); }
        return "caja/historial";
    }
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id,
                          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
                          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
                          Model model,RedirectAttributes redirect) {
        var rango=fechas(desde,hasta);
        try {
            HistorialCajaService.validarFechas(rango[0],rango[1]);
            model.addAttribute("detalle",historial.detalle(id));model.addAttribute("desde",rango[0]);model.addAttribute("hasta",rango[1]);
            return "caja/historial-detalle";
        } catch(IllegalArgumentException ex) {
            redirect.addFlashAttribute("error",ex.getMessage());return "redirect:/caja/historial";
        }
    }
    private LocalDate[] fechas(LocalDate desde,LocalDate hasta) {
        var hoy=LocalDate.now(ZoneId.of("America/Lima"));
        return new LocalDate[]{desde==null?hoy.withDayOfMonth(1):desde,hasta==null?hoy:hasta};
    }
}
