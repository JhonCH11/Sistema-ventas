package com.minimarket.stock.controller;

import com.minimarket.stock.service.AlertaStockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AlertaStockController {
    private final AlertaStockService alertas;
    public AlertaStockController(AlertaStockService alertas) { this.alertas=alertas; }
    @GetMapping("/stock/alertas")
    public String mostrar(Model model) {
        model.addAttribute("alertasStock",alertas.consultar());
        return "stock/alertas";
    }
}
