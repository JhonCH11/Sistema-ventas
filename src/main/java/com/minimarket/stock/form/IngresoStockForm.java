package com.minimarket.stock.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class IngresoStockForm {

    @NotNull(message = "Selecciona un producto.")
    private Long productoId;

    @NotNull(message = "Ingresa la cantidad.")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero.")
    private BigDecimal cantidad;

    @NotNull(message = "Ingresa el costo de compra.")
    @DecimalMin(value = "0.01", message = "El costo debe ser mayor a cero.")
    private BigDecimal costoUnitario;

    @Size(max = 300, message = "La observación no puede superar 300 caracteres.")
    private String observacion;

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
