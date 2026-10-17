package com.minimarket.compras.controller;

import com.minimarket.compras.form.ProveedorForm;
import com.minimarket.compras.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/compras/proveedores")
public class ProveedorController {
    private final ProveedorService servicio;
    public ProveedorController(ProveedorService servicio){this.servicio=servicio;}
    @GetMapping public String lista(Model model){model.addAttribute("proveedores",servicio.listar());return "compras/proveedores";}
    @GetMapping("/nuevo") public String nuevo(Model model){model.addAttribute("proveedorForm",new ProveedorForm());return "compras/proveedor-formulario";}
    @GetMapping("/{id}/editar") public String editar(@PathVariable Long id,Model model,RedirectAttributes redirect){
        try{var p=servicio.obtener(id);var f=new ProveedorForm();f.setNombre(p.nombre());f.setTelefono(p.telefono());f.setActivo(p.activo());model.addAttribute("proveedorForm",f);model.addAttribute("proveedorId",id);return "compras/proveedor-formulario";}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("error",ex.getMessage());return "redirect:/compras/proveedores";}
    }
    @PostMapping({"/nuevo","/{id}/editar"})
    public String guardar(@PathVariable(required=false) Long id,@Valid @ModelAttribute ProveedorForm proveedorForm,BindingResult errores,Model model,RedirectAttributes redirect){
        model.addAttribute("proveedorId",id);
        if(!errores.hasErrors())try{servicio.guardar(id,proveedorForm);redirect.addFlashAttribute("mensaje","Proveedor guardado.");return "redirect:/compras/proveedores";}
        catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());}
        catch(DataIntegrityViolationException ex){model.addAttribute("error","Ya existe un proveedor con ese nombre.");}
        return "compras/proveedor-formulario";
    }
}
