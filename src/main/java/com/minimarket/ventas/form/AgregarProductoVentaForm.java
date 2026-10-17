package com.minimarket.ventas.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AgregarProductoVentaForm {

    @NotNull(message = "Selecciona un producto.")
    private Long productoId;

    @NotNull(message = "Ingresa la cantidad o el peso.")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero.")
    private BigDecimal cantidad;

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
}
