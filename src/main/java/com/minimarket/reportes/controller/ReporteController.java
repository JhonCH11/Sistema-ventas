package com.minimarket.reportes.controller;
import com.minimarket.reportes.service.ReporteService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/reportes")
public class ReporteController {
    private final ReporteService servicio;
    public ReporteController(ReporteService servicio) { this.servicio=servicio; }
    @GetMapping
    public String mostrar(
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate hoy=LocalDate.now(ZoneId.of("America/Lima"));
        desde=desde==null?hoy:desde; hasta=hasta==null?hoy:hasta;
        model.addAttribute("desde",desde);model.addAttribute("hasta",hasta);
        try { model.addAttribute("reporte",servicio.consultar(desde,hasta)); }
        catch(IllegalArgumentException ex) { model.addAttribute("error",ex.getMessage()); }
        return "reportes/principal";
    }
}

