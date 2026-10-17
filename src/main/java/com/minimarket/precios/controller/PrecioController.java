package com.minimarket.precios.controller;

import com.minimarket.precios.service.PrecioService;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/precios")
public class PrecioController {

    private final PrecioService precioService;

    public PrecioController(PrecioService precioService) {
        this.precioService = precioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", precioService.listarProductosActivos());
        return "precios/lista";
    }

    @PostMapping("/{productoId}")
    public String actualizar(
            @PathVariable Long productoId,
            @RequestParam String nuevoPrecio,
            Authentication autenticacion,
            RedirectAttributes atributos) {
        try {
            BigDecimal precio = new BigDecimal(nuevoPrecio.trim().replace(',', '.'));
            precioService.actualizarPrecio(productoId, precio, autenticacion.getName());
            atributos.addFlashAttribute("mensaje", "Precio actualizado.");
        } catch (IllegalArgumentException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/precios";
    }
}
