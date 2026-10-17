package com.minimarket.productos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_venta", nullable = false)
    private TipoVenta tipoVenta;

    @Column(name = "precio_actual", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioActual;

    @Column(name = "stock_actual", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "es_liquidacion", nullable = false)
    private boolean liquidacion;

    @Column(name = "precio_referencia_liquidacion", precision = 12, scale = 2)
    private BigDecimal precioReferenciaLiquidacion;

    public boolean isLiquidacion() { return liquidacion; }
    public BigDecimal getPrecioReferenciaLiquidacion() { return precioReferenciaLiquidacion; }
    public void marcarLiquidacion(BigDecimal precioNormal) {
        liquidacion = true;
        precioReferenciaLiquidacion = precioNormal;
    }

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    protected Producto() {
    }

    public Producto(
            Categoria categoria,
            String nombre,
            TipoVenta tipoVenta,
            BigDecimal precioActual,
            BigDecimal stockActual,
            BigDecimal stockMinimo) {
        this.categoria = categoria;
        this.nombre = nombre;
        this.tipoVenta = tipoVenta;
        this.precioActual = precioActual;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
    }

    public Long getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoVenta getTipoVenta() { return tipoVenta; }
    public void setTipoVenta(TipoVenta tipoVenta) { this.tipoVenta = tipoVenta; }
    public BigDecimal getPrecioActual() { return precioActual; }
    public void setPrecioActual(BigDecimal precioActual) { this.precioActual = precioActual; }
    public BigDecimal getStockActual() { return stockActual; }
    public void setStockActual(BigDecimal stockActual) { this.stockActual = stockActual; }
    public BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo = stockMinimo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
