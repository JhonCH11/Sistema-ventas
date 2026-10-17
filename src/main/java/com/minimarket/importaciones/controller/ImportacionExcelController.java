package com.minimarket.importaciones.controller;

import com.minimarket.importaciones.form.ImportacionExcel.*;
import com.minimarket.importaciones.service.*;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/importaciones/excel")
public class ImportacionExcelController {
    private static final String SESION="vistaPreviaExcel";
    private final ImportacionExcelService servicio;private final LectorExcelService lector;
    public ImportacionExcelController(ImportacionExcelService servicio,LectorExcelService lector){this.servicio=servicio;this.lector=lector;}
    @GetMapping public String mostrar(){return "importaciones/excel";}
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla(@RequestParam Tipo tipo){
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=plantilla-"+tipo.name().toLowerCase(Locale.ROOT)+".xlsx").body(lector.plantilla(tipo));
    }
    @PostMapping("/revisar")
    public String revisar(@RequestParam Tipo tipo,@RequestParam String referencia,@RequestParam MultipartFile archivo,Authentication auth,HttpSession sesion,Model model){
        sesion.removeAttribute(SESION);model.addAttribute("tipo",tipo);model.addAttribute("referencia",referencia);
        try{
            if(archivo.getOriginalFilename()==null||!archivo.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")||archivo.getSize()>LectorExcelService.MAX_BYTES)throw new IllegalArgumentException("Seleccione un archivo .xlsx de hasta 2 MB.");
            var previa=servicio.preparar(tipo,referencia,archivo.getBytes(),auth.getName());sesion.setAttribute(SESION,previa);model.addAttribute("previa",previa);
        }catch(IllegalArgumentException|IOException ex){model.addAttribute("error",ex instanceof IOException?"No se pudo leer el archivo.":ex.getMessage());}
        return "importaciones/excel";
    }
    @PostMapping("/confirmar")
    public String confirmar(@RequestParam String token,Authentication auth,HttpSession sesion,Model model,RedirectAttributes redirect){
        if(servicio.registrada(token,auth.getName())){redirect.addFlashAttribute("mensaje","La importacion ya estaba registrada. No se duplicaron datos.");return "redirect:/importaciones/excel";}
        var previa=(Previa)sesion.getAttribute(SESION);
        if(previa==null||!previa.token().equals(token)){redirect.addFlashAttribute("error","No hay una vista previa valida. Cargue el archivo nuevamente.");return "redirect:/importaciones/excel";}
        try{
            servicio.confirmar(previa,auth.getName());sesion.removeAttribute(SESION);redirect.addFlashAttribute("mensaje","Importacion completada: "+previa.filas().size()+" filas registradas.");return "redirect:/importaciones/excel";
        }catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());}
        catch(DataIntegrityViolationException ex){
            if(servicio.registrada(token,auth.getName())){redirect.addFlashAttribute("mensaje","La importacion ya estaba registrada. No se duplicaron datos.");return "redirect:/importaciones/excel";}
            model.addAttribute("error","La referencia, el archivo o un producto ya estan registrados. No se importo ninguna fila.");
        }
        model.addAttribute("previa",previa);return "importaciones/excel";
    }
    @PostMapping("/cancelar")
    public String cancelar(HttpSession sesion){sesion.removeAttribute(SESION);return "redirect:/importaciones/excel";}
}
