package com.minimarket.stock.domain;

import com.minimarket.productos.domain.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "ingreso_detalles")
public class IngresoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingreso_mercaderia_id", nullable = false)
    private IngresoMercaderia ingresoMercaderia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name="producto_nombre",length=150)
    private String productoNombre;
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name="tipo_venta")
    private com.minimarket.productos.domain.TipoVenta tipoVenta;

    protected IngresoDetalle() {
    }

    public IngresoDetalle(
            IngresoMercaderia ingresoMercaderia,
            Producto producto,
            BigDecimal cantidad,
            BigDecimal costoUnitario) {
        this.ingresoMercaderia = ingresoMercaderia;
        this.producto = producto;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.productoNombre = producto.getNombre();
        this.tipoVenta = producto.getTipoVenta();
    }

    public Long getId() { return id; }
}
