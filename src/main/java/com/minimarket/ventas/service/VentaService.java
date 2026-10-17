package com.minimarket.ventas.service;

import com.minimarket.caja.domain.AperturaCaja;
import com.minimarket.caja.service.CajaService;
import com.minimarket.productos.domain.Producto;
import com.minimarket.productos.domain.TipoVenta;
import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.stock.domain.MovimientoStock;
import com.minimarket.stock.domain.TipoMovimientoStock;
import com.minimarket.stock.repository.MovimientoStockRepository;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.service.UsuarioService;
import com.minimarket.ventas.domain.MedioPago;
import com.minimarket.ventas.domain.Pago;
import com.minimarket.ventas.domain.Venta;
import com.minimarket.ventas.domain.VentaDetalle;
import com.minimarket.ventas.form.CarritoVenta;
import com.minimarket.ventas.form.ConfirmarVentaForm;
import com.minimarket.ventas.form.LineaCarritoVenta;
import com.minimarket.ventas.repository.PagoRepository;
import com.minimarket.ventas.repository.VentaDetalleRepository;
import com.minimarket.ventas.repository.VentaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VentaService {

    private final ProductoRepository productoRepository;
    private final CajaService cajaService;
    private final UsuarioService usuarioService;
    private final VentaRepository ventaRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final PagoRepository pagoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final com.minimarket.ventas.repository.CorreccionVentaRepository correcciones;

    public VentaService(
            ProductoRepository productoRepository,
            CajaService cajaService,
            UsuarioService usuarioService,
            VentaRepository ventaRepository,
            VentaDetalleRepository ventaDetalleRepository,
            PagoRepository pagoRepository,
            MovimientoStockRepository movimientoStockRepository,
            com.minimarket.ventas.repository.CorreccionVentaRepository correcciones) {
        this.productoRepository = productoRepository;
        this.cajaService = cajaService;
        this.usuarioService = usuarioService;
        this.ventaRepository = ventaRepository;
        this.ventaDetalleRepository = ventaDetalleRepository;
        this.pagoRepository = pagoRepository;
        this.movimientoStockRepository = movimientoStockRepository;
        this.correcciones = correcciones;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductosDisponibles() {
        return productoRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional
    public void agregarAlCarrito(CarritoVenta carrito, Long productoId, BigDecimal cantidad) {
        cajaService.obtenerAperturaAbiertaDeHoy();
        Producto producto = productoRepository.findById(productoId)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("El producto seleccionado no está disponible."));
        validarCantidad(producto.getTipoVenta(), cantidad);

        BigDecimal cantidadEnCarrito = carrito.getLineas().stream()
                .filter(linea -> linea.getProductoId().equals(productoId))
                .map(LineaCarritoVenta::getCantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (producto.getStockActual().compareTo(cantidadEnCarrito.add(cantidad)) < 0) {
            throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre()
                    + ". Disponible: " + producto.getStockActual() + unidad(producto.getTipoVenta()) + ".");
        }
        carrito.agregar(producto, cantidad);
    }

    @Transactional
    public VentaConfirmada confirmarVenta(
            CarritoVenta carrito,
            ConfirmarVentaForm formulario,
            String nombreUsuario) {
        synchronized (carrito) {
            if (carrito.estaVacio()) {
                throw new IllegalArgumentException("Agrega al menos un producto antes de confirmar la venta.");
            }

            AperturaCaja apertura = cajaService.bloquearAperturaDeHoy();
            if (!carrito.getTokenConfirmacion().equals(formulario.getToken())) {
                throw new IllegalArgumentException("La venta cambio. Revisa el carrito antes de cobrar.");
            }
            if (ventaRepository.findByNumeroTicket(numeroTicket(carrito)).isPresent()) {
                throw new IllegalArgumentException("Esta venta ya fue registrada.");
            }
            Usuario vendedor = usuarioService.obtenerUsuarioActivo(nombreUsuario);
            Map<Long, Producto> productos = buscarProductosParaVenta(carrito.getLineas());

            BigDecimal total = calcularTotalYValidarStock(carrito.getLineas(), productos);
            if (total.signum() <= 0) {
                throw new IllegalArgumentException("El total debe ser mayor a cero.");
            }
            PagoCalculado pago = calcularPago(formulario, total);
            Venta venta = ventaRepository.save(new Venta(apertura, vendedor, numeroTicket(carrito), total));

            for (LineaCarritoVenta linea : carrito.getLineas()) {
                Producto producto = productos.get(linea.getProductoId());
                BigDecimal subtotal = linea.getPrecioUnitario().multiply(linea.getCantidad())
                        .setScale(2, RoundingMode.HALF_UP);
                ventaDetalleRepository.save(new VentaDetalle(
                        venta,
                        producto,
                        producto.getNombre(),
                        producto.getTipoVenta(),
                        linea.getCantidad(),
                        linea.getPrecioUnitario(),
                        subtotal));
                producto.setStockActual(producto.getStockActual().subtract(linea.getCantidad()));

                MovimientoStock movimiento = new MovimientoStock(
                        producto,
                        vendedor,
                        TipoMovimientoStock.VENTA,
                        linea.getCantidad().negate(),
                        "Venta confirmada");
                movimiento.setReferenciaTipo("VENTA");
                movimiento.setReferenciaId(venta.getId());
                movimientoStockRepository.save(movimiento);
            }

            pagoRepository.save(new Pago(
                    venta,
                    formulario.getMedioPago(),
                    total,
                    pago.importeRecibido(),
                    pago.vuelto()));
            return new VentaConfirmada(venta.getNumeroTicket(), total, pago.vuelto());
        }
    }

    private Map<Long, Producto> buscarProductosParaVenta(Collection<LineaCarritoVenta> lineas) {
        List<Long> ids = lineas.stream().map(LineaCarritoVenta::getProductoId).sorted().toList();
        Map<Long, Producto> productos = productoRepository.findAllByIdInForUpdate(ids).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        if (productos.size() != ids.size()) {
            throw new IllegalArgumentException("Uno de los productos ya no está disponible.");
        }
        return productos;
    }

    private BigDecimal calcularTotalYValidarStock(
            Collection<LineaCarritoVenta> lineas,
            Map<Long, Producto> productos) {
        BigDecimal total = BigDecimal.ZERO;
        for (LineaCarritoVenta linea : lineas) {
            Producto producto = productos.get(linea.getProductoId());
            if (!producto.isActivo()) {
                throw new IllegalArgumentException("El producto " + producto.getNombre() + " ya no está disponible.");
            }
            if (producto.getTipoVenta() != linea.getTipoVenta()) {
                throw new IllegalArgumentException("Cambio la forma de venta. Retira y agrega nuevamente el producto.");
            }
            validarCantidad(producto.getTipoVenta(), linea.getCantidad());
            if (producto.getStockActual().compareTo(linea.getCantidad()) < 0) {
                throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre()
                        + ". Disponible: " + producto.getStockActual() + unidad(producto.getTipoVenta()) + ".");
            }
            total = total.add(linea.getPrecioUnitario().multiply(linea.getCantidad())
                    .setScale(2, RoundingMode.HALF_UP));
        }
        return total;
    }

    private PagoCalculado calcularPago(ConfirmarVentaForm formulario, BigDecimal total) {
        if (formulario.getMedioPago() == null) {
            throw new IllegalArgumentException("Selecciona el medio de pago.");
        }
        if (formulario.getMedioPago() == MedioPago.EFECTIVO) {
            BigDecimal recibido = formulario.getImporteRecibido();
            if (recibido == null) {
                throw new IllegalArgumentException("Ingresa el efectivo recibido.");
            }
            validarImporte(recibido, "El efectivo recibido");
            if (recibido.compareTo(total) < 0) {
                throw new IllegalArgumentException("El efectivo recibido no cubre el total de la venta.");
            }
            return new PagoCalculado(recibido, recibido.subtract(total));
        }
        if (!formulario.isAbonoVerificado()) {
            throw new IllegalArgumentException("Verifica el abono de Yape o Plin antes de confirmar.");
        }
        return new PagoCalculado(null, BigDecimal.ZERO);
    }

    private void validarCantidad(TipoVenta tipoVenta, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }
        if (tipoVenta == TipoVenta.UNIDAD && cantidad.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Los productos por unidad deben usar cantidades enteras.");
        }
        if (tipoVenta == TipoVenta.PESO && cantidad.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Los productos por peso permiten como máximo dos decimales.");
        }
    }

    private void validarImporte(BigDecimal importe, String etiqueta) {
        if (importe.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(etiqueta + " permite como máximo dos decimales.");
        }
    }

    private String numeroTicket(CarritoVenta carrito) {
        return "T-" + carrito.getTokenConfirmacion();
    }

    @Transactional(readOnly = true)
    public void actualizarCantidad(CarritoVenta carrito, Long id, BigDecimal cantidad) {
        cajaService.obtenerAperturaAbiertaDeHoy();
        Producto producto = productoRepository.findById(id).filter(Producto::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("Producto no disponible."));
        validarCantidad(producto.getTipoVenta(), cantidad);
        if (cantidad.compareTo(producto.getStockActual()) > 0) {
            throw new IllegalArgumentException("Stock insuficiente. Disponible: " + producto.getStockActual());
        }
        carrito.actualizar(id, cantidad);
    }

    @Transactional(readOnly = true)
    public Ticket consultarTicket(String numero) {
        Venta venta = ventaRepository.findByNumeroTicket(numero)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada."));
        return new Ticket(venta, ventaDetalleRepository.findByVentaIdOrderByIdAsc(venta.getId()),
                pagoRepository.findByVentaId(venta.getId()).orElseThrow());
    }

    public record Ticket(Venta venta, List<VentaDetalle> detalles, Pago pago) {}

    @Transactional(readOnly = true)
    public List<Venta> listarPorFecha(java.time.LocalDate fecha) {
        return ventaRepository.findByVendidaEnGreaterThanEqualAndVendidaEnLessThanOrderByVendidaEnDesc(
                fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public BigDecimal totalNeto(List<Venta> ventas) {
        return ventas.stream().filter(v -> v.getEstado() == com.minimarket.ventas.domain.EstadoVenta.CONFIRMADA)
                .map(v -> v.getTotal().subtract(correcciones.totalDevuelto(v.getId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String unidad(TipoVenta tipoVenta) {
        return tipoVenta == TipoVenta.PESO ? " kg" : " unidades";
    }

    private record PagoCalculado(BigDecimal importeRecibido, BigDecimal vuelto) {
    }

    public record VentaConfirmada(String numeroTicket, BigDecimal total, BigDecimal vuelto) {
    }
}
