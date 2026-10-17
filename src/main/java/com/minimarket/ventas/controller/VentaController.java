package com.minimarket.ventas.controller;

import com.minimarket.caja.service.CajaService;
import com.minimarket.configuracion.service.ConfiguracionNegocioService;
import com.minimarket.ventas.form.AgregarProductoVentaForm;
import com.minimarket.ventas.form.CarritoVenta;
import com.minimarket.ventas.form.ConfirmarVentaForm;
import com.minimarket.ventas.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ventas")
@SessionAttributes("carrito")
public class VentaController {

    private final VentaService ventaService;
    private final CajaService cajaService;
    private final ConfiguracionNegocioService configuracionService;

    public VentaController(VentaService ventaService, CajaService cajaService,
                           ConfiguracionNegocioService configuracionService) {
        this.ventaService = ventaService;
        this.cajaService = cajaService;
        this.configuracionService = configuracionService;
    }

    @ModelAttribute("carrito")
    public CarritoVenta crearCarrito() {
        return new CarritoVenta();
    }

    @GetMapping
    public String mostrarNuevaVenta(@ModelAttribute("carrito") CarritoVenta carrito, Model model) {
        try {
            cajaService.obtenerAperturaAbiertaDeHoy();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("mensaje", ex.getMessage());
            return "ventas/sin-caja";
        }
        prepararPantalla(model, new AgregarProductoVentaForm(), new ConfirmarVentaForm());
        return "ventas/punto-venta";
    }

    @PostMapping("/carrito")
    public String agregarProducto(
            @Valid @ModelAttribute("agregarFormulario") AgregarProductoVentaForm formulario,
            BindingResult resultado,
            @ModelAttribute("carrito") CarritoVenta carrito,
            Model model) {
        if (resultado.hasErrors()) {
            prepararPantalla(model, formulario, new ConfirmarVentaForm());
            return "ventas/punto-venta";
        }
        try {
            ventaService.agregarAlCarrito(carrito, formulario.getProductoId(), formulario.getCantidad());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorGeneral", ex.getMessage());
            prepararPantalla(model, formulario, new ConfirmarVentaForm());
            return "ventas/punto-venta";
        }
        return "redirect:/ventas";
    }

    @PostMapping("/carrito/{productoId}/retirar")
    public String retirarProducto(@PathVariable Long productoId, @ModelAttribute("carrito") CarritoVenta carrito) {
        carrito.retirar(productoId);
        return "redirect:/ventas";
    }

    @PostMapping("/confirmar")
    public String confirmarVenta(
            @Valid @ModelAttribute("confirmarFormulario") ConfirmarVentaForm formulario,
            BindingResult resultado,
            @ModelAttribute("carrito") CarritoVenta carrito,
            Authentication autenticacion,
            Model model,
            RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            prepararPantalla(model, new AgregarProductoVentaForm(), formulario);
            return "ventas/punto-venta";
        }
        try {
            VentaService.VentaConfirmada venta = ventaService.confirmarVenta(carrito, formulario, autenticacion.getName());
            carrito.limpiar();
            atributos.addFlashAttribute("mensaje", "Venta confirmada. Ticket " + venta.numeroTicket()
                    + ". Total: S/ " + venta.total() + ". Vuelto: S/ " + venta.vuelto());
            return "redirect:/ventas/ticket/" + venta.numeroTicket();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorGeneral", ex.getMessage());
            prepararPantalla(model, new AgregarProductoVentaForm(), formulario);
            return "ventas/punto-venta";
        }
    }

    private void prepararPantalla(
            Model model,
            AgregarProductoVentaForm agregarFormulario,
            ConfirmarVentaForm confirmarFormulario) {
        if (!model.containsAttribute("agregarFormulario")) {
            model.addAttribute("agregarFormulario", agregarFormulario);
        }
        if (!model.containsAttribute("confirmarFormulario")) {
            model.addAttribute("confirmarFormulario", confirmarFormulario);
        }
        model.addAttribute("productos", ventaService.listarProductosDisponibles());
    }

    @PostMapping("/carrito/{id}/cantidad")
    public String actualizarCantidad(@PathVariable Long id, @RequestParam BigDecimal cantidad,
            @ModelAttribute("carrito") CarritoVenta carrito, RedirectAttributes atributos) {
        try {
            ventaService.actualizarCantidad(carrito, id, cantidad);
        } catch (IllegalArgumentException ex) {
            atributos.addFlashAttribute("errorGeneral", ex.getMessage());
        }
        return "redirect:/ventas";
    }

    @PostMapping("/cancelar")
    public String cancelar(@ModelAttribute("carrito") CarritoVenta carrito) {
        carrito.limpiar();
        return "redirect:/ventas";
    }

    @GetMapping("/ticket/{numero}")
    public String ticket(@PathVariable String numero, Model model) {
        model.addAttribute("negocio", configuracionService.obtener());
        model.addAttribute("ticket", ventaService.consultarTicket(numero));
        return "ventas/ticket";
    }

    @GetMapping("/realizadas")
    public String realizadas(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso =
                    org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fecha,
            Model model) {
        java.time.LocalDate dia = fecha == null
                ? java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")) : fecha;
        var ventas = ventaService.listarPorFecha(dia);
        model.addAttribute("fecha", dia);
        model.addAttribute("ventas", ventas);
        model.addAttribute("totalVigente", ventaService.totalNeto(ventas));
        return "ventas/realizadas";
    }
}
