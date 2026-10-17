package com.minimarket.compras.controller;

import com.minimarket.compras.form.CompraForm;
import com.minimarket.compras.service.*;
import com.minimarket.stock.form.IngresoStockForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.*;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/compras")
public class CompraController {
    private final CompraService servicio;private final ProveedorService proveedores;
    public CompraController(CompraService servicio,ProveedorService proveedores){this.servicio=servicio;this.proveedores=proveedores;}
    @InitBinder("compraForm") void limitar(WebDataBinder binder){binder.setAutoGrowCollectionLimit(50);}
    @GetMapping public String lista(@RequestParam(required=false) LocalDate desde,@RequestParam(required=false) LocalDate hasta,Model model){
        var hoy=LocalDate.now(ZoneId.of("America/Lima"));if(desde==null)desde=hoy.withDayOfMonth(1);if(hasta==null)hasta=hoy;
        model.addAttribute("desde",desde);model.addAttribute("hasta",hasta);
        try{model.addAttribute("compras",servicio.listar(desde,hasta));}catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());model.addAttribute("compras",java.util.List.of());}
        return "compras/lista";
    }
    @GetMapping("/nueva") public String nueva(Model model,HttpSession sesion){
        var f=new CompraForm();f.setToken(UUID.randomUUID().toString());f.getLineas().add(new IngresoStockForm());sesion.setAttribute("tokenCompra",f.getToken());
        model.addAttribute("compraForm",f);opciones(model);return "compras/formulario";
    }
    @PostMapping("/confirmar") public String confirmar(@Valid @ModelAttribute CompraForm compraForm,BindingResult errores,Authentication auth,HttpSession sesion,Model model,RedirectAttributes redirect){
        var previa=servicio.registrada(compraForm.getToken(),auth.getName());if(previa.isPresent())return "redirect:/compras/"+previa.get();
        if(!java.util.Objects.equals(sesion.getAttribute("tokenCompra"),compraForm.getToken())){
            redirect.addFlashAttribute("error","Abra una nueva compra. Esta pantalla ya no esta vigente.");return "redirect:/compras";
        }
        if(!errores.hasErrors())try{
            var id=servicio.confirmar(compraForm,auth.getName());sesion.removeAttribute("tokenCompra");redirect.addFlashAttribute("mensaje","Compra registrada y stock actualizado.");return "redirect:/compras/"+id;
        }catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());}
        catch(DataIntegrityViolationException ex){
            var registrada=servicio.registrada(compraForm.getToken(),auth.getName());if(registrada.isPresent())return "redirect:/compras/"+registrada.get();
            model.addAttribute("error","Ese documento del proveedor ya esta registrado o los datos cambiaron. No se registro la compra.");
        }
        opciones(model);return "compras/formulario";
    }
    @GetMapping("/{id}") public String detalle(@PathVariable Long id,Model model,RedirectAttributes redirect){
        try{model.addAttribute("compra",servicio.obtener(id));model.addAttribute("detalles",servicio.detalles(id));return "compras/detalle";}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("error",ex.getMessage());return "redirect:/compras";}
    }
    private void opciones(Model model){model.addAttribute("productos",servicio.productos());model.addAttribute("proveedores",proveedores.listar().stream().filter(p->p.activo()).toList());}
}
