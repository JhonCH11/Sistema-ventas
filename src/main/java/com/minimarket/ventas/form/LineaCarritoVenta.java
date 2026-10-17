package com.minimarket.ventas.form;

import com.minimarket.productos.domain.TipoVenta;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class LineaCarritoVenta implements Serializable {

    private final Long productoId;
    private final String nombreProducto;
    private final TipoVenta tipoVenta;
    private final BigDecimal precioUnitario;
    private BigDecimal cantidad;

    public LineaCarritoVenta(
            Long productoId,
            String nombreProducto,
            TipoVenta tipoVenta,
            BigDecimal precioUnitario,
            BigDecimal cantidad) {
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.tipoVenta = tipoVenta;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
    }

    public void sumarCantidad(BigDecimal cantidad) {
        this.cantidad = this.cantidad.add(cantidad);
    }

    public Long getProductoId() { return productoId; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getNombreProducto() { return nombreProducto; }
    public TipoVenta getTipoVenta() { return tipoVenta; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getCantidad() { return cantidad; }
    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
    }
}
