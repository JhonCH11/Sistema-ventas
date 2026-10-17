package com.minimarket.stock.controller;
import com.minimarket.stock.form.AjusteStockForm;
import com.minimarket.stock.service.StockService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/stock/ajustes")
public class AjusteStockController {
    private final StockService servicio;
    public AjusteStockController(StockService servicio){this.servicio=servicio;}
    @GetMapping("/{id}")
    public String mostrar(@PathVariable Long id, Model model) {
        var producto=servicio.obtenerProducto(id);
        var form=new AjusteStockForm();
        form.setStockEsperado(producto.getStockActual());
        model.addAttribute("producto",producto);model.addAttribute("formulario",form);
        return "stock/ajuste";
    }
    @PostMapping("/{id}")
    public String guardar(@PathVariable Long id,@Valid @ModelAttribute("formulario") AjusteStockForm form,
            BindingResult errores,Authentication auth,Model model,RedirectAttributes flash) {
        if(!errores.hasErrors()) {
            try {
                servicio.ajustarStock(id,form,auth.getName());
                flash.addFlashAttribute("mensaje","Ajuste guardado con su motivo.");
                return "redirect:/stock";
            } catch(IllegalArgumentException ex){model.addAttribute("errorGeneral",ex.getMessage());}
        }
        model.addAttribute("producto",servicio.obtenerProducto(id));
        return "stock/ajuste";
    }
}

