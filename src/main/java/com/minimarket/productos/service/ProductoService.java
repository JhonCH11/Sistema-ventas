package com.minimarket.productos.service;

import com.minimarket.productos.domain.Categoria;
import com.minimarket.productos.domain.Producto;
import com.minimarket.productos.domain.TipoVenta;
import com.minimarket.productos.form.CategoriaForm;
import com.minimarket.productos.form.ProductoEdicionForm;
import com.minimarket.productos.form.ProductoForm;
import com.minimarket.productos.repository.CategoriaRepository;
import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.stock.domain.MovimientoStock;
import com.minimarket.stock.domain.TipoMovimientoStock;
import com.minimarket.stock.repository.MovimientoStockRepository;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.service.UsuarioService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final UsuarioService usuarioService;

    public ProductoService(
            CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository,
            MovimientoStockRepository movimientoStockRepository,
            UsuarioService usuarioService) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.movimientoStockRepository = movimientoStockRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarCategoriasActivas() {
        return categoriaRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public ProductoEdicionForm obtenerFormularioEdicion(Long productoId) {
        Producto producto = obtenerProductoActivo(productoId);
        ProductoEdicionForm formulario = new ProductoEdicionForm();
        formulario.setNombre(producto.getNombre());
        formulario.setCategoriaId(producto.getCategoria().getId());
        formulario.setTipoVenta(producto.getTipoVenta());
        formulario.setStockMinimo(producto.getStockMinimo());
        return formulario;
    }

    @Transactional
    public void crearCategoria(CategoriaForm formulario) {
        String nombre = formulario.getNombre().trim();
        if (categoriaRepository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new IllegalArgumentException("Esa categoría ya existe.");
        }
        categoriaRepository.save(new Categoria(nombre));
    }

    @Transactional
    public void crearProducto(ProductoForm formulario, String nombreUsuario) {
        String nombre = formulario.getNombre().trim();
        if (productoRepository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new IllegalArgumentException("Ese producto ya existe.");
        }

        validarCantidadPorTipo(formulario.getTipoVenta(), formulario.getStockInicial());
        validarCantidadPorTipo(formulario.getTipoVenta(), formulario.getStockMinimo());

        Categoria categoria = categoriaRepository.findById(formulario.getCategoriaId())
                .filter(Categoria::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("La categoría seleccionada no está disponible."));
        Usuario usuario = usuarioService.obtenerUsuarioActivo(nombreUsuario);

        Producto producto = new Producto(
                categoria,
                nombre,
                formulario.getTipoVenta(),
                formulario.getPrecioActual(),
                formulario.getStockInicial(),
                formulario.getStockMinimo());
        productoRepository.save(producto);

        if (formulario.getStockInicial().compareTo(BigDecimal.ZERO) > 0) {
            MovimientoStock movimiento = new MovimientoStock(
                    producto,
                    usuario,
                    TipoMovimientoStock.STOCK_INICIAL,
                    formulario.getStockInicial(),
                    "Stock inicial");
            movimiento.setReferenciaTipo("PRODUCTO");
            movimiento.setReferenciaId(producto.getId());
            movimientoStockRepository.save(movimiento);
        }
    }

    @Transactional
    public void actualizarProducto(Long productoId, ProductoEdicionForm formulario) {
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("El producto ya no esta disponible."));
        if (producto.isLiquidacion()) throw new IllegalArgumentException("La presentacion en liquidacion conserva su nombre y unidad. Cambie su precio desde Precios del dia.");
        String nombre = formulario.getNombre().trim();

        productoRepository.findByNombreIgnoreCase(nombre)
                .filter(encontrado -> !encontrado.getId().equals(productoId))
                .ifPresent(encontrado -> {
                    throw new IllegalArgumentException("Ese nombre ya pertenece a otro producto.");
                });

        validarCantidadPorTipo(formulario.getTipoVenta(), formulario.getStockMinimo());
        if (producto.getTipoVenta() != formulario.getTipoVenta()
                && producto.getStockActual().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException(
                    "No se puede cambiar la forma de venta mientras el producto tenga stock.");
        }

        Categoria categoria = categoriaRepository.findById(formulario.getCategoriaId())
                .filter(Categoria::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("La categoría seleccionada no está disponible."));

        producto.setNombre(nombre);
        producto.setCategoria(categoria);
        producto.setTipoVenta(formulario.getTipoVenta());
        producto.setStockMinimo(formulario.getStockMinimo());
    }

    private Producto obtenerProductoActivo(Long productoId) {
        return productoRepository.findById(productoId)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("El producto ya no está disponible."));
    }

    private void validarCantidadPorTipo(TipoVenta tipoVenta, BigDecimal cantidad) {
        if (tipoVenta == TipoVenta.UNIDAD && cantidad.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Los productos por unidad deben usar cantidades enteras.");
        }
        if (tipoVenta == TipoVenta.PESO && cantidad.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Los productos por peso permiten como máximo dos decimales.");
        }
    }
}
