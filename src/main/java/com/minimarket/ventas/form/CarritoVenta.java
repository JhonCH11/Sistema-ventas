package com.minimarket.ventas.form;

import com.minimarket.productos.domain.Producto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CarritoVenta implements Serializable {

    private final List<LineaCarritoVenta> lineas = new ArrayList<>();
    private String tokenConfirmacion = nuevoToken();

    public void agregar(Producto producto, BigDecimal cantidad) {
        lineas.stream()
                .filter(linea -> linea.getProductoId().equals(producto.getId()))
                .findFirst()
                .ifPresentOrElse(
                        linea -> linea.sumarCantidad(cantidad),
                        () -> lineas.add(new LineaCarritoVenta(
                                producto.getId(),
                                producto.getNombre(),
                                producto.getTipoVenta(),
                                producto.getPrecioActual(),
                                cantidad)));
    }

    public void retirar(Long productoId) {
        lineas.removeIf(linea -> linea.getProductoId().equals(productoId));
    }

    public void actualizar(Long productoId, BigDecimal cantidad) {
        lineas.stream().filter(linea -> linea.getProductoId().equals(productoId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Producto no encontrado en la venta."))
                .setCantidad(cantidad);
    }

    public void limpiar() {
        lineas.clear();
        tokenConfirmacion = nuevoToken();
    }

    public List<LineaCarritoVenta> getLineas() { return List.copyOf(lineas); }
    public boolean estaVacio() { return lineas.isEmpty(); }
    public String getTokenConfirmacion() { return tokenConfirmacion; }
    public BigDecimal getTotal() {
        return lineas.stream()
                .map(LineaCarritoVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String nuevoToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
