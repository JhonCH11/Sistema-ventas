package com.minimarket.precios.service;

import com.minimarket.precios.domain.HistorialPrecio;
import com.minimarket.precios.repository.HistorialPrecioRepository;
import com.minimarket.productos.domain.Producto;
import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.service.UsuarioService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrecioService {

    private final ProductoRepository productoRepository;
    private final HistorialPrecioRepository historialPrecioRepository;
    private final UsuarioService usuarioService;

    public PrecioService(
            ProductoRepository productoRepository,
            HistorialPrecioRepository historialPrecioRepository,
            UsuarioService usuarioService) {
        this.productoRepository = productoRepository;
        this.historialPrecioRepository = historialPrecioRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductosActivos() {
        return productoRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional
    public void actualizarPrecio(Long productoId, BigDecimal nuevoPrecio, String nombreUsuario) {
        validarPrecio(nuevoPrecio);

        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("El producto ya no está disponible."));

        if (producto.isLiquidacion() && nuevoPrecio.compareTo(producto.getPrecioReferenciaLiquidacion()) >= 0) {
            throw new IllegalArgumentException("El precio de liquidacion debe ser menor al precio normal registrado al separar la mercaderia.");
        }

        if (producto.getPrecioActual().compareTo(nuevoPrecio) == 0) {
            throw new IllegalArgumentException("El nuevo precio es igual al precio actual.");
        }

        Usuario usuario = usuarioService.obtenerUsuarioActivo(nombreUsuario);
        historialPrecioRepository.save(new HistorialPrecio(
                producto,
                usuario,
                producto.getPrecioActual(),
                nuevoPrecio));
        producto.setPrecioActual(nuevoPrecio);
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero.");
        }
        if (precio.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El precio permite como máximo dos decimales.");
        }
    }
}
