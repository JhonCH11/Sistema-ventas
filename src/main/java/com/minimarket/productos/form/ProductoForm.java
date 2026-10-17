package com.minimarket.productos.form;

import com.minimarket.productos.domain.TipoVenta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ProductoForm {

    @NotBlank(message = "Ingresa el nombre del producto.")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres.")
    private String nombre;

    @NotNull(message = "Selecciona una categoría.")
    private Long categoriaId;

    @NotNull(message = "Selecciona si se vende por unidad o por peso.")
    private TipoVenta tipoVenta;

    @NotNull(message = "Ingresa el precio de venta.")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero.")
    private BigDecimal precioActual;

    @NotNull(message = "Ingresa el stock inicial.")
    @DecimalMin(value = "0.00", message = "El stock inicial no puede ser negativo.")
    private BigDecimal stockInicial = BigDecimal.ZERO;

    @NotNull(message = "Ingresa el stock mínimo.")
    @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo.")
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public TipoVenta getTipoVenta() { return tipoVenta; }
    public void setTipoVenta(TipoVenta tipoVenta) { this.tipoVenta = tipoVenta; }
    public BigDecimal getPrecioActual() { return precioActual; }
    public void setPrecioActual(BigDecimal precioActual) { this.precioActual = precioActual; }
    public BigDecimal getStockInicial() { return stockInicial; }
    public void setStockInicial(BigDecimal stockInicial) { this.stockInicial = stockInicial; }
    public BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo = stockMinimo; }
}
