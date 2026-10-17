package com.minimarket.stock.controller;

import com.minimarket.stock.form.*;
import com.minimarket.stock.service.LiquidacionMermaService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/stock")
public class LiquidacionMermaController {
    private final LiquidacionMermaService service;
    public LiquidacionMermaController(LiquidacionMermaService service){this.service=service;}
    @GetMapping("/liquidaciones")
    public String saldos(Model model){model.addAttribute("saldos",service.saldos());return "stock/liquidaciones";}
    @GetMapping("/liquidaciones/nueva")
    public String nueva(@RequestParam(required=false) Long productoId,Model model){
        if(productoId==null)return seleccionar(model,true);
        var producto=service.obtener(productoId);
        if(producto.isLiquidacion())throw new IllegalArgumentException("El producto ya pertenece a una liquidacion.");
        var form=new LiquidacionForm();form.setProductoId(productoId);form.setToken(UUID.randomUUID().toString());form.setCostoUnitario(service.costo(producto));
        model.addAttribute("formulario",form);preparar(model,productoId);return "stock/liquidacion-formulario";
    }
    @PostMapping("/liquidaciones")
    public String liquidar(@Valid @ModelAttribute("formulario") LiquidacionForm form,BindingResult errores,Authentication auth,Model model,RedirectAttributes redirect){
        if(!errores.hasErrors())try{
            service.liquidar(form,auth.getName());redirect.addFlashAttribute("mensaje","Cantidad separada para liquidacion. Ya esta disponible en Nueva venta.");return "redirect:/stock/liquidaciones";
        }catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());}
        catch(org.springframework.dao.DataIntegrityViolationException ex){model.addAttribute("error","No se pudo guardar. Consulte las liquidaciones antes de reintentar; puede existir otro registro con el mismo nombre o token.");}
        preparar(model,form.getProductoId());return "stock/liquidacion-formulario";
    }
    @GetMapping("/mermas/nueva")
    public String nuevaMerma(@RequestParam(required=false) Long productoId,Model model){
        if(productoId==null)return seleccionar(model,false);
        var producto=service.obtener(productoId);
        var form=new MermaForm();form.setProductoId(productoId);form.setToken(UUID.randomUUID().toString());form.setCostoUnitario(service.costo(producto));
        model.addAttribute("formulario",form);preparar(model,productoId);return "stock/merma-formulario";
    }
    @PostMapping("/mermas")
    public String merma(@Valid @ModelAttribute("formulario") MermaForm form,BindingResult errores,Authentication auth,Model model,RedirectAttributes redirect){
        if(!errores.hasErrors())try{
            service.registrarMerma(form,auth.getName());redirect.addFlashAttribute("mensaje","Merma registrada. Stock descontado y perdida guardada.");return "redirect:/stock/mermas";
        }catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());}
        catch(org.springframework.dao.DataIntegrityViolationException ex){model.addAttribute("error","No se pudo guardar. Consulte el registro de mermas antes de reintentar.");}
        preparar(model,form.getProductoId());return "stock/merma-formulario";
    }
    @GetMapping("/mermas")
    public String mermas(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,Model model){
        var hoy=LocalDate.now(ZoneId.of("America/Lima"));desde=desde==null?hoy.withDayOfMonth(1):desde;hasta=hasta==null?hoy:hasta;
        model.addAttribute("desde",desde);model.addAttribute("hasta",hasta);
        try{
            var filas=service.mermas(desde,hasta);model.addAttribute("mermas",filas);
            model.addAttribute("total",filas.stream().map(m->m.perdida()).reduce(BigDecimal.ZERO,BigDecimal::add));
            Map<String,BigDecimal> porProducto=new TreeMap<>(),porMotivo=new TreeMap<>();
            filas.forEach(m->{porProducto.merge(m.base(),m.perdida(),BigDecimal::add);porMotivo.merge(m.motivo(),m.perdida(),BigDecimal::add);});
            model.addAttribute("porProducto",porProducto);model.addAttribute("porMotivo",porMotivo);
        }catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());}
        return "stock/mermas";
    }
    private String seleccionar(Model model,boolean liquidacion){
        model.addAttribute("titulo",liquidacion?"Nueva liquidacion":"Registrar merma");
        model.addAttribute("ruta",liquidacion?"/stock/liquidaciones/nueva":"/stock/mermas/nueva");
        model.addAttribute("productos",liquidacion?service.productosNormales().stream().filter(p->p.getStockActual().signum()>0).toList():service.productosConStock());
        return "stock/seleccionar-producto";
    }
    private void preparar(Model model,Long id){
        var p=service.obtener(id);model.addAttribute("producto",p);model.addAttribute("unidad",p.getTipoVenta()==com.minimarket.productos.domain.TipoVenta.PESO?"kg":"unidades");
        model.addAttribute("paso",p.getTipoVenta()==com.minimarket.productos.domain.TipoVenta.PESO?"0.01":"1");
        model.addAttribute("hoy",LocalDate.now(ZoneId.of("America/Lima")));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public String entradaInvalida(IllegalArgumentException ex,RedirectAttributes redirect){
        redirect.addFlashAttribute("error",ex.getMessage());return "redirect:/stock/liquidaciones";
    }
}
