package com.minimarket.ventas.controller;

import com.minimarket.ventas.service.CorreccionVentaService;
import com.minimarket.ventas.service.VentaService;
import com.minimarket.ventas.repository.CorreccionVentaRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ventas/ticket/{numero}/correcciones")
public class CorreccionVentaController {
    private final VentaService ventas;
    private final CorreccionVentaService servicio;
    private final CorreccionVentaRepository repositorio;
    public CorreccionVentaController(VentaService ventas,CorreccionVentaService servicio,CorreccionVentaRepository repositorio) {
        this.ventas=ventas;this.servicio=servicio;this.repositorio=repositorio;
    }
    @GetMapping
    public String mostrar(@PathVariable String numero,Model model) {
        var ticket=ventas.consultarTicket(numero);
        model.addAttribute("ticket",ticket);
        model.addAttribute("historial",repositorio.historial(ticket.venta().getId()));
        model.addAttribute("token",UUID.randomUUID().toString());
        return "ventas/correcciones";
    }
    @PostMapping("/anular")
    public String anular(@PathVariable String numero,@RequestParam String administrador,
            @RequestParam String clave,@RequestParam String motivo,
            @RequestParam(defaultValue="") String observacion,RedirectAttributes flash) {
        try {
            servicio.anular(numero,administrador,clave,motivo,observacion);
            flash.addFlashAttribute("mensaje","Venta anulada. Stock repuesto.");
        } catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/ventas/ticket/"+numero+"/correcciones";
    }
    @PostMapping("/devolver")
    public String devolver(@PathVariable String numero,@RequestParam Long detalle,
            @RequestParam BigDecimal cantidad,@RequestParam String administrador,
            @RequestParam String clave,@RequestParam String motivo,@RequestParam String token,
            @RequestParam(required=false) Boolean reponeStock,
            @RequestParam(defaultValue="") String observacion,RedirectAttributes flash) {
        try {
            servicio.devolver(numero,detalle,cantidad,administrador,clave,motivo,observacion,token,reponeStock);
            flash.addFlashAttribute("mensaje","Devolucion registrada. Revisa el importe de reembolso en el historial.");
        } catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/ventas/ticket/"+numero+"/correcciones";
    }
}

