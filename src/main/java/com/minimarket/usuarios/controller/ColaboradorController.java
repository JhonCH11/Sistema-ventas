package com.minimarket.usuarios.controller;

import com.minimarket.usuarios.form.ColaboradorForm;
import com.minimarket.usuarios.service.ColaboradorService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/colaboradores")
public class ColaboradorController {
    private final ColaboradorService service;
    public ColaboradorController(ColaboradorService service) { this.service = service; }
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("colaboradores", service.listar());
        return "usuarios/colaboradores";
    }
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("formulario", new ColaboradorForm());
        return "usuarios/colaborador-formulario";
    }
    @PostMapping("/nuevo")
    public String crear(@Valid @ModelAttribute("formulario") ColaboradorForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                service.crear(form);
                redirect.addFlashAttribute("mensaje", "Colaborador registrado. Ya puede iniciar sesion.");
                return "redirect:/colaboradores";
            } catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); }
            catch (DataIntegrityViolationException ex) { model.addAttribute("error", "Ese usuario ya esta en uso."); }
        }
        return "usuarios/colaborador-formulario";
    }
    @PostMapping("/{id}/estado")
    public String estado(@PathVariable Long id, @RequestParam boolean activo, RedirectAttributes redirect) {
        try {
            service.cambiarEstado(id, activo);
            redirect.addFlashAttribute("mensaje", activo ? "Acceso activado." : "Acceso desactivado.");
        } catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/colaboradores";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        var usuario = service.obtener(id);
        var form = new ColaboradorForm();
        form.setNombre(usuario.getNombre());
        form.setNombreUsuario(usuario.getNombreUsuario());
        model.addAttribute("formulario", form);
        model.addAttribute("colaboradorId", id);
        return "usuarios/colaborador-formulario";
    }

    @PostMapping("/{id}/editar")
    public String guardarEdicion(@PathVariable Long id, @Valid @ModelAttribute("formulario") ColaboradorForm form,
                                 BindingResult resultado, Model model, RedirectAttributes redirect) {
        model.addAttribute("colaboradorId", id);
        if (!resultado.hasErrors()) {
            try {
                service.editar(id, form);
                redirect.addFlashAttribute("mensaje", "Datos actualizados. Si cambio el usuario, el colaborador debe ingresar con el nuevo usuario y su misma contrasena.");
                return "redirect:/colaboradores";
            } catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); }
            catch (DataIntegrityViolationException ex) { model.addAttribute("error", "Ese usuario ya esta en uso."); }
        }
        return "usuarios/colaborador-formulario";
    }
    @GetMapping("/{id}/contrasena")
    public String clave(@PathVariable Long id, Model model) {
        model.addAttribute("colaborador", service.obtener(id));
        model.addAttribute("formulario", new ColaboradorForm());
        return "usuarios/colaborador-clave";
    }
    @PostMapping("/{id}/contrasena")
    public String clave(@PathVariable Long id, @ModelAttribute("formulario") ColaboradorForm form,
                        Model model, RedirectAttributes redirect) {
        try {
            service.cambiarContrasena(id, form);
            redirect.addFlashAttribute("mensaje", "Contrasena actualizada.");
            return "redirect:/colaboradores";
        } catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); }
        model.addAttribute("colaborador", service.obtener(id));
        return "usuarios/colaborador-clave";
    }
}
