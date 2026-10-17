package com.minimarket.stock.controller;

import com.minimarket.stock.form.IngresoStockForm;
import com.minimarket.stock.service.StockService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public String mostrarPantalla(Model model, @RequestParam(required=false) Long productoId) {
        var formulario = new IngresoStockForm();
        formulario.setProductoId(productoId);
        prepararPantalla(model, formulario);
        return "stock/lista";
    }

    @ModelAttribute("administrador")
    public boolean esAdministrador(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    @PostMapping("/ingresos")
    public String registrarIngreso(
            @Valid @ModelAttribute("formulario") IngresoStockForm formulario,
            BindingResult resultado,
            Authentication autenticacion,
            Model model,
            RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            prepararPantalla(model, formulario);
            return "stock/lista";
        }
        try {
            stockService.registrarIngreso(formulario, autenticacion.getName());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorGeneral", ex.getMessage());
            prepararPantalla(model, formulario);
            return "stock/lista";
        }
        atributos.addFlashAttribute("mensaje", "Ingreso de stock guardado.");
        return "redirect:/stock";
    }

    private void prepararPantalla(Model model, IngresoStockForm formulario) {
        model.addAttribute("formulario", formulario);
        model.addAttribute("productos", stockService.listarProductosActivos());
        model.addAttribute("movimientos", stockService.ultimosMovimientos());
    }
}
