package com.minimarket.reportes.controller;

import com.minimarket.reportes.service.ResumenDiarioService;
import java.time.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ResumenDiarioController {
    private final ResumenDiarioService resumen;
    public ResumenDiarioController(ResumenDiarioService resumen) {this.resumen=resumen;}
    @GetMapping("/reportes/resumen-diario")
    public String mostrar(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fecha,Model model) {
        fecha=fecha==null?LocalDate.now(ZoneId.of("America/Lima")):fecha;model.addAttribute("fecha",fecha);
        try{model.addAttribute("resumen",resumen.consultar(fecha));}catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());}
        return "reportes/resumen-diario";
    }
}
