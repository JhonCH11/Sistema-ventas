package com.minimarket.productos.controller;

import com.minimarket.productos.domain.TipoVenta;
import com.minimarket.productos.form.CategoriaForm;
import com.minimarket.productos.form.ProductoForm;
import com.minimarket.productos.form.ProductoEdicionForm;
import com.minimarket.productos.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoService.listarActivos());
        return "productos/lista";
    }

    @GetMapping("/categorias/nueva")
    public String mostrarNuevaCategoria(Model model) {
        model.addAttribute("formulario", new CategoriaForm());
        return "productos/categoria-formulario";
    }

    @PostMapping("/categorias")
    public String crearCategoria(
            @Valid @ModelAttribute("formulario") CategoriaForm formulario,
            BindingResult resultado,
            RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            return "productos/categoria-formulario";
        }
        try {
            productoService.crearCategoria(formulario);
        } catch (IllegalArgumentException ex) {
            resultado.rejectValue("nombre", "categoria.duplicada", ex.getMessage());
            return "productos/categoria-formulario";
        }
        atributos.addFlashAttribute("mensaje", "Categoría guardada.");
        return "redirect:/productos";
    }

    @GetMapping("/nuevo")
    public String mostrarNuevoProducto(Model model) {
        prepararFormularioProducto(model, new ProductoForm());
        return "productos/formulario";
    }

    @PostMapping
    public String crearProducto(
            @Valid @ModelAttribute("formulario") ProductoForm formulario,
            BindingResult resultado,
            Authentication autenticacion,
            Model model,
            RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            prepararFormularioProducto(model, formulario);
            return "productos/formulario";
        }
        try {
            productoService.crearProducto(formulario, autenticacion.getName());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorGeneral", ex.getMessage());
            prepararFormularioProducto(model, formulario);
            return "productos/formulario";
        }
        atributos.addFlashAttribute("mensaje", "Producto y stock inicial guardados.");
        return "redirect:/productos";
    }

    @GetMapping("/{productoId}/editar")
    public String mostrarEdicion(@PathVariable Long productoId, Model model, RedirectAttributes atributos) {
        try {
            prepararFormularioEdicion(model, productoService.obtenerFormularioEdicion(productoId));
            model.addAttribute("productoId", productoId);
            return "productos/edicion-formulario";
        } catch (IllegalArgumentException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
            return "redirect:/productos";
        }
    }

    @PostMapping("/{productoId}/editar")
    public String actualizarProducto(
            @PathVariable Long productoId,
            @Valid @ModelAttribute("formulario") ProductoEdicionForm formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            prepararFormularioEdicion(model, formulario);
            model.addAttribute("productoId", productoId);
            return "productos/edicion-formulario";
        }
        try {
            productoService.actualizarProducto(productoId, formulario);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorGeneral", ex.getMessage());
            prepararFormularioEdicion(model, formulario);
            model.addAttribute("productoId", productoId);
            return "productos/edicion-formulario";
        }
        atributos.addFlashAttribute("mensaje", "Producto actualizado.");
        return "redirect:/productos";
    }

    private void prepararFormularioProducto(Model model, ProductoForm formulario) {
        model.addAttribute("formulario", formulario);
        model.addAttribute("categorias", productoService.listarCategoriasActivas());
        model.addAttribute("tiposVenta", TipoVenta.values());
    }

    private void prepararFormularioEdicion(Model model, ProductoEdicionForm formulario) {
        model.addAttribute("formulario", formulario);
        model.addAttribute("categorias", productoService.listarCategoriasActivas());
        model.addAttribute("tiposVenta", TipoVenta.values());
    }
}
