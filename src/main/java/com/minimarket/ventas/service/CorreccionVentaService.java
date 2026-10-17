package com.minimarket.ventas.service;

import com.minimarket.caja.service.CajaService;
import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.stock.domain.MovimientoStock;
import com.minimarket.stock.domain.TipoMovimientoStock;
import com.minimarket.stock.repository.MovimientoStockRepository;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.domain.RolUsuario;
import com.minimarket.usuarios.repository.UsuarioRepository;
import com.minimarket.ventas.domain.EstadoVenta;
import com.minimarket.ventas.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorreccionVentaService {
    private final VentaRepository ventas;
    private final VentaDetalleRepository detalles;
    private final CorreccionVentaRepository correcciones;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final CajaService cajas;
    private final ProductoRepository productos;
    private final MovimientoStockRepository movimientos;
    public CorreccionVentaService(VentaRepository ventas, VentaDetalleRepository detalles,
            CorreccionVentaRepository correcciones, UsuarioRepository usuarios, PasswordEncoder encoder,
            CajaService cajas, ProductoRepository productos, MovimientoStockRepository movimientos) {
        this.ventas=ventas; this.detalles=detalles; this.correcciones=correcciones;
        this.usuarios=usuarios; this.encoder=encoder; this.cajas=cajas; this.productos=productos; this.movimientos=movimientos;
    }
    private Usuario autorizar(String nombre, String clave) {
        Usuario admin=usuarios.findByNombreUsuarioAndActivoTrue(nombre)
                .filter(u -> u.getRol()==RolUsuario.ADMINISTRADOR)
                .orElseThrow(() -> new IllegalArgumentException("Autorizacion de administrador incorrecta."));
        if(!encoder.matches(clave,admin.getContrasenaHash()))
            throw new IllegalArgumentException("Autorizacion de administrador incorrecta.");
        return admin;
    }
    @Transactional
    public void anular(String numero,String nombre,String clave,String motivo,String observacion) {
        if(!Set.of("ERROR_REGISTRO","CLIENTE_CANCELO","PAGO_NO_CONFIRMADO").contains(motivo))
            throw new IllegalArgumentException("Selecciona un motivo valido.");
        validarObservacion(observacion);
        Usuario admin=autorizar(nombre,clave);
        var caja=cajas.bloquearAperturaDeHoy();
        var venta=ventas.bloquearPorTicket(numero).orElseThrow();
        if(venta.getEstado()!=EstadoVenta.CONFIRMADA)
            throw new IllegalArgumentException("La venta ya fue anulada.");
        if(!venta.getAperturaCaja().getId().equals(caja.getId()))
            throw new IllegalArgumentException("Esta venta pertenece a otra caja. Registra una devolucion.");
        if(correcciones.tieneDevoluciones(venta.getId()))
            throw new IllegalArgumentException("La venta tiene devoluciones. Devuelve los productos restantes.");
        var lineas=detalles.findByVentaIdOrderByIdAsc(venta.getId());
        productos.findAllByIdInForUpdate(lineas.stream().map(d->d.getProducto().getId()).sorted().toList());
        for(var d:lineas) {
            var p=d.getProducto();
            p.setStockActual(p.getStockActual().add(d.getCantidad()));
            MovimientoStock m=new MovimientoStock(p,admin,TipoMovimientoStock.ANULACION,d.getCantidad(),motivo);
            m.setReferenciaTipo("VENTA"); m.setReferenciaId(venta.getId()); movimientos.save(m);
        }
        correcciones.anular(venta.getId(),admin.getId(),motivo,observacion);
        venta.setEstado(EstadoVenta.ANULADA);
    }
    @Transactional
    public void devolver(String numero,Long detalle,BigDecimal cantidad,String nombre,String clave,
            String motivo,String observacion,String token,Boolean reponeStock) {
        validarDestinoStock(motivo, reponeStock);
        if(!Set.of("PRODUCTO_MAL_ESTADO","PRODUCTO_EQUIVOCADO","CLIENTE_CAMBIO_OPINION").contains(motivo))
            throw new IllegalArgumentException("Selecciona un motivo valido.");
        validarObservacion(observacion);
        UUID.fromString(token);
        Usuario admin=autorizar(nombre,clave);
        var caja=cajas.bloquearAperturaDeHoy();
        var venta=ventas.bloquearPorTicket(numero).orElseThrow();
        if(correcciones.existeToken(token)) throw new IllegalArgumentException("Esta devolucion ya fue registrada.");
        if(venta.getEstado()!=EstadoVenta.CONFIRMADA) throw new IllegalArgumentException("La venta esta anulada.");
        var d=detalles.findByVentaIdOrderByIdAsc(venta.getId()).stream().filter(x->x.getId().equals(detalle))
                .findFirst().orElseThrow(()->new IllegalArgumentException("Producto no encontrado en esta venta."));
        if(cantidad.signum()<=0 || cantidad.stripTrailingZeros().scale()>2 ||
                (d.getTipoVenta().name().equals("UNIDAD") && cantidad.stripTrailingZeros().scale()>0))
            throw new IllegalArgumentException("Cantidad invalida. Usa unidades enteras o peso con dos decimales.");
        BigDecimal anterior=correcciones.cantidadDevuelta(detalle);
        BigDecimal restante=d.getCantidad().subtract(anterior);
        if(cantidad.compareTo(restante)>0) throw new IllegalArgumentException("Cantidad disponible para devolver: "+restante);
        BigDecimal importe=cantidad.equals(restante) || cantidad.compareTo(restante)==0
                ? d.getSubtotal().subtract(correcciones.importeDevuelto(detalle))
                : cantidad.multiply(d.getPrecioUnitario()).setScale(2,RoundingMode.HALF_UP)
                    .min(d.getSubtotal().subtract(correcciones.importeDevuelto(detalle)));
        boolean repone=reponeStock;
        correcciones.devolver(venta.getId(),admin.getId(),caja.getId(),token,motivo,observacion,detalle,cantidad,importe,repone);
        if(repone) {
            var p=productos.findByIdForUpdate(d.getProducto().getId()).orElseThrow();
            p.setStockActual(p.getStockActual().add(cantidad));
            MovimientoStock m=new MovimientoStock(p,admin,TipoMovimientoStock.DEVOLUCION,cantidad,motivo);
            m.setReferenciaTipo("VENTA");m.setReferenciaId(venta.getId());movimientos.save(m);
        }
    }
    private void validarObservacion(String texto) {
        if(texto!=null && texto.length()>300) throw new IllegalArgumentException("La observacion permite hasta 300 caracteres.");
    }

    public static void validarDestinoStock(String motivo, Boolean reponeStock) {
        if (reponeStock == null)
            throw new IllegalArgumentException("El administrador debe indicar el estado del producto devuelto.");
        if ("PRODUCTO_MAL_ESTADO".equals(motivo) && reponeStock)
            throw new IllegalArgumentException("Un producto en mal estado no puede volver al stock vendible.");
    }
}

