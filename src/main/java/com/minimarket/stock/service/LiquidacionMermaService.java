package com.minimarket.stock.service;

import com.minimarket.productos.domain.*;
import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.stock.domain.*;
import com.minimarket.stock.form.*;
import com.minimarket.stock.repository.*;
import com.minimarket.precios.domain.HistorialPrecio;
import com.minimarket.precios.repository.HistorialPrecioRepository;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.service.UsuarioService;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class LiquidacionMermaService {
    private final ProductoRepository productos;
    private final LiquidacionMermaRepository registros;
    private final MovimientoStockRepository movimientos;
    private final HistorialPrecioRepository precios;
    private final UsuarioService usuarios;
    public LiquidacionMermaService(ProductoRepository productos,LiquidacionMermaRepository registros,MovimientoStockRepository movimientos,HistorialPrecioRepository precios,UsuarioService usuarios){
        this.productos=productos;this.registros=registros;this.movimientos=movimientos;this.precios=precios;this.usuarios=usuarios;
    }
    @Transactional(readOnly=true) public List<Producto> productosNormales(){return productos.findByActivoTrueOrderByNombreAsc().stream().filter(p->!p.isLiquidacion()).toList();}
    @Transactional(readOnly=true) public List<Producto> productosConStock(){return productos.findByActivoTrueOrderByNombreAsc().stream().filter(p->p.getStockActual().signum()>0).toList();}
    @Transactional(readOnly=true) public List<LiquidacionMermaRepository.Saldo> saldos(){return registros.saldos();}
    @Transactional(readOnly=true) public Producto obtener(Long id){return productos.findById(id).filter(Producto::isActivo).orElseThrow(()->new IllegalArgumentException("Producto no disponible."));}
    @Transactional(readOnly=true) public BigDecimal costo(Producto p){return p.isLiquidacion()?registros.costoLiquidacion(p.getId()):registros.costoReciente(p.getId());}
    @Transactional(readOnly=true) public List<LiquidacionMermaRepository.Merma> mermas(LocalDate desde,LocalDate hasta){
        if(desde==null||hasta==null||desde.isAfter(hasta)||hasta.equals(LocalDate.MAX)) throw new IllegalArgumentException("Revise el rango de fechas.");
        return registros.mermas(desde,hasta);
    }
    @Transactional public void liquidar(LiquidacionForm form,String nombreUsuario){
        var usuario=administrador(nombreUsuario); validarToken(form.getToken());
        var origen=bloquear(form.getProductoId());
        if(registros.existeLiquidacion(form.getToken())) return;
        if(origen.isLiquidacion()) throw new IllegalArgumentException("Este producto ya pertenece a una liquidacion.");
        validarCantidad(origen,form.getCantidad()); validarImporte(form.getPrecio()); validarImporte(form.getCostoUnitario());
        if(form.getPrecio().compareTo(origen.getPrecioActual())>=0) throw new IllegalArgumentException("El precio de liquidacion debe ser menor al precio normal.");
        if(form.getMotivo()==null||form.getMotivo().isBlank()||form.getMotivo().length()>200) throw new IllegalArgumentException("Indique un motivo de hasta 200 caracteres.");
        var ahora=LocalDateTime.now(ZoneId.of("America/Lima"));
        if(form.getFechaLlegada()!=null&&form.getFechaLlegada().isAfter(ahora.toLocalDate())) throw new IllegalArgumentException("La fecha de llegada no puede ser futura.");
        long registro=registros.crearLiquidacion(form,usuario.getId(),ahora);
        String nombre=origen.getNombre().substring(0,Math.min(110,origen.getNombre().length()))+" (Liquidacion L"+registro+")";
        var saldo=new Producto(origen.getCategoria(),nombre,origen.getTipoVenta(),form.getPrecio(),form.getCantidad(),BigDecimal.ZERO);
        saldo.marcarLiquidacion(origen.getPrecioActual());
        origen.setStockActual(origen.getStockActual().subtract(form.getCantidad()));
        productos.saveAndFlush(saldo); registros.vincular(registro,saldo.getId());
        precios.save(new HistorialPrecio(saldo,usuario,form.getPrecio(),form.getPrecio()));
        movimiento(origen,usuario,form.getCantidad().negate(),"Salida a liquidacion","LIQUIDACION",registro);
        movimiento(saldo,usuario,form.getCantidad(),"Entrada a liquidacion","LIQUIDACION",registro);
    }
    @Transactional public void registrarMerma(MermaForm form,String nombreUsuario){
        var usuario=administrador(nombreUsuario); validarToken(form.getToken());
        var producto=bloquear(form.getProductoId());
        if(registros.existeMerma(form.getToken())) return;
        validarCantidad(producto,form.getCantidad());
        if(form.getMotivo()==null) throw new IllegalArgumentException("Seleccione el motivo de la merma.");
        if(form.getObservacion()!=null&&form.getObservacion().length()>200) throw new IllegalArgumentException("La observacion admite hasta 200 caracteres.");
        BigDecimal costo=producto.isLiquidacion()?registros.costoLiquidacion(producto.getId()):form.getCostoUnitario();
        validarImporte(costo);
        BigDecimal perdida=form.getCantidad().multiply(costo).setScale(2,RoundingMode.HALF_UP);
        if(perdida.precision()-perdida.scale()>10) throw new IllegalArgumentException("El importe de perdida es demasiado grande.");
        producto.setStockActual(producto.getStockActual().subtract(form.getCantidad()));
        long registro=registros.crearMerma(form,producto,usuario.getId(),costo,perdida,LocalDateTime.now(ZoneId.of("America/Lima")));
        movimiento(producto,usuario,form.getCantidad().negate(),"Merma: "+form.getMotivo().name(),"MERMA",registro);
    }
    private Producto bloquear(Long id){return productos.findByIdForUpdate(id).filter(Producto::isActivo).orElseThrow(()->new IllegalArgumentException("Producto no disponible."));}
    private Usuario administrador(String nombre){var u=usuarios.obtenerUsuarioActivo(nombre);if(u.getRol()!=com.minimarket.usuarios.domain.RolUsuario.ADMINISTRADOR)throw new IllegalArgumentException("Solo el administrador puede gestionar liquidaciones y mermas.");return u;}
    private void movimiento(Producto p,Usuario u,BigDecimal cantidad,String motivo,String referencia,Long id){
        var m=new MovimientoStock(p,u,cantidad.signum()>0?TipoMovimientoStock.AJUSTE_POSITIVO:TipoMovimientoStock.AJUSTE_NEGATIVO,cantidad,motivo);
        m.setReferenciaTipo(referencia);m.setReferenciaId(id);movimientos.save(m);
    }
    private void validarToken(String token){try{if(!UUID.fromString(token).toString().equals(token))throw new IllegalArgumentException();}catch(Exception ex){throw new IllegalArgumentException("Actualice el formulario antes de registrar.");}}
    public static void validarCantidad(Producto p,BigDecimal cantidad){
        validarImporte(cantidad);
        if(p.getTipoVenta()==TipoVenta.UNIDAD&&cantidad.stripTrailingZeros().scale()>0)throw new IllegalArgumentException("Ingrese unidades enteras.");
        if(cantidad.compareTo(p.getStockActual())>0)throw new IllegalArgumentException("Stock insuficiente. Disponible: "+p.getStockActual());
    }
    private static void validarImporte(BigDecimal valor){
        if(valor==null||valor.signum()<=0||valor.stripTrailingZeros().scale()>2||valor.precision()-valor.scale()>10)throw new IllegalArgumentException("Ingrese un valor positivo con hasta dos decimales y diez cifras enteras.");
    }
}
