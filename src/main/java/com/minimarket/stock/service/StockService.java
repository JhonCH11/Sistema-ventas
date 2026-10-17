package com.minimarket.stock.service;

import com.minimarket.productos.domain.Producto;
import com.minimarket.productos.domain.TipoVenta;
import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.stock.domain.IngresoDetalle;
import com.minimarket.stock.domain.IngresoMercaderia;
import com.minimarket.stock.domain.MovimientoStock;
import com.minimarket.stock.domain.TipoMovimientoStock;
import com.minimarket.stock.form.IngresoStockForm;
import com.minimarket.stock.repository.IngresoDetalleRepository;
import com.minimarket.stock.repository.IngresoMercaderiaRepository;
import com.minimarket.stock.repository.MovimientoStockRepository;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.service.UsuarioService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final ProductoRepository productoRepository;
    private final IngresoMercaderiaRepository ingresoMercaderiaRepository;
    private final IngresoDetalleRepository ingresoDetalleRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final UsuarioService usuarioService;

    public StockService(
            ProductoRepository productoRepository,
            IngresoMercaderiaRepository ingresoMercaderiaRepository,
            IngresoDetalleRepository ingresoDetalleRepository,
            MovimientoStockRepository movimientoStockRepository,
            UsuarioService usuarioService) {
        this.productoRepository = productoRepository;
        this.ingresoMercaderiaRepository = ingresoMercaderiaRepository;
        this.ingresoDetalleRepository = ingresoDetalleRepository;
        this.movimientoStockRepository = movimientoStockRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductosActivos() {
        return productoRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional
    public void registrarIngreso(IngresoStockForm formulario, String nombreUsuario) {
        Usuario usuario = usuarioService.obtenerUsuarioActivo(nombreUsuario);
        String observacion = textoOpcional(formulario.getObservacion());
        IngresoMercaderia ingreso = ingresoMercaderiaRepository.save(new IngresoMercaderia(usuario, observacion));
        registrarDetalle(ingreso, formulario.getProductoId(), formulario.getCantidad(), formulario.getCostoUnitario());
    }

    @Transactional
    public void registrarDetalle(IngresoMercaderia ingreso, Long productoId, BigDecimal cantidad, BigDecimal costo) {
        if (productoId == null) throw new IllegalArgumentException("Seleccione un producto.");
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("El producto seleccionado no está disponible."));
        validarPositivo(cantidad, "La cantidad");
        validarCantidad(producto.getTipoVenta(), cantidad);
        if (producto.isLiquidacion()) throw new IllegalArgumentException("Para agregar productos en liquidacion, separe una nueva cantidad desde Productos en liquidacion.");
        validarPositivo(costo, "El costo");
        validarCosto(costo);
        if (producto.getStockActual().add(cantidad).compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new IllegalArgumentException("El stock resultante es demasiado grande: " + producto.getNombre());
        Usuario usuario = ingreso.getUsuario();
        String observacion = ingreso.getObservacion();
        ingresoDetalleRepository.save(new IngresoDetalle(
                ingreso,
                producto,
                cantidad,
                costo));

        producto.setStockActual(producto.getStockActual().add(cantidad));
        MovimientoStock movimiento = new MovimientoStock(
                producto,
                usuario,
                TipoMovimientoStock.INGRESO_MERCADERIA,
                cantidad,
                "Ingreso de mercadería");
        movimiento.setObservacion(observacion);
        movimiento.setReferenciaTipo("INGRESO_MERCADERIA");
        movimiento.setReferenciaId(ingreso.getId());
        movimientoStockRepository.save(movimiento);
    }

    private void validarPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0 || valor.stripTrailingZeros().scale() > 2
                || valor.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new IllegalArgumentException(campo + " debe ser positiva, con hasta dos decimales y diez cifras enteras.");
    }

    private void validarCantidad(TipoVenta tipoVenta, BigDecimal cantidad) {
        if (tipoVenta == TipoVenta.UNIDAD && cantidad.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Los productos por unidad deben usar cantidades enteras.");
        }
        if (tipoVenta == TipoVenta.PESO && cantidad.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Los productos por peso permiten como máximo dos decimales.");
        }
    }

    private void validarCosto(BigDecimal costo) {
        if (costo.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El costo permite como máximo dos decimales.");
        }
    }

    private String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    @Transactional(readOnly = true)
    public Producto obtenerProducto(Long id) {
        return productoRepository.findById(id).filter(Producto::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("Producto no disponible."));
    }

    @Transactional(readOnly = true)
    public List<MovimientoStock> ultimosMovimientos() {
        return movimientoStockRepository.findTop30ByOrderByIdDesc();
    }

    @Transactional
    public void ajustarStock(Long id, com.minimarket.stock.form.AjusteStockForm form, String nombreUsuario) {
        Usuario usuario = usuarioService.obtenerUsuarioActivo(nombreUsuario);
        if (usuario.getRol() != com.minimarket.usuarios.domain.RolUsuario.ADMINISTRADOR)
            throw new IllegalArgumentException("Solo el administrador puede ajustar stock.");
        Producto producto = productoRepository.findByIdForUpdate(id).filter(Producto::isActivo).orElseThrow();
        if (producto.isLiquidacion()) throw new IllegalArgumentException("Los productos en liquidacion se retiran desde Mermas, no desde ajustes.");
        if (producto.getStockActual().compareTo(form.getStockEsperado()) != 0)
            throw new IllegalArgumentException("El stock cambio. Vuelve a Stock y abre nuevamente el ajuste.");
        if (form.getCantidad()==null || form.getCantidad().signum()<0)
            throw new IllegalArgumentException("Ingresa una cantidad no negativa.");
        validarCantidad(producto.getTipoVenta(),form.getCantidad());
        BigDecimal diferencia = calcularDiferencia(producto.getStockActual(),form.getCantidad(),form.getMotivo());
        producto.setStockActual(producto.getStockActual().add(diferencia));
        MovimientoStock movimiento = new MovimientoStock(producto,usuario,
                diferencia.signum()>0 ? TipoMovimientoStock.AJUSTE_POSITIVO : TipoMovimientoStock.AJUSTE_NEGATIVO,
                diferencia,form.getMotivo().name());
        movimiento.setObservacion(textoOpcional(form.getObservacion()));
        movimientoStockRepository.save(movimiento);
    }

    public static BigDecimal calcularDiferencia(BigDecimal actual,BigDecimal cantidad,
            com.minimarket.stock.form.AjusteStockForm.Motivo motivo) {
        if (motivo == null) throw new IllegalArgumentException("Selecciona un motivo.");
        BigDecimal diferencia = motivo == com.minimarket.stock.form.AjusteStockForm.Motivo.CONTEO_FISICO
                ? cantidad.subtract(actual) : cantidad.negate();
        if (diferencia.signum()==0) throw new IllegalArgumentException("No hay diferencia de stock para registrar.");
        if (actual.add(diferencia).signum()<0) throw new IllegalArgumentException("No puedes retirar mas que el stock disponible.");
        return diferencia;
    }
}
