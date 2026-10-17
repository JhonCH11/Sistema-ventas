package com.minimarket.ventas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false, unique = true)
    private Venta venta;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private MedioPago medioPago;

    @Column(name = "importe_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal importePagado;

    @Column(name = "importe_recibido", precision = 12, scale = 2)
    private BigDecimal importeRecibido;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal vuelto = BigDecimal.ZERO;

    @Column(name = "pagado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime pagadoEn;

    protected Pago() {
    }

    public Pago(
            Venta venta,
            MedioPago medioPago,
            BigDecimal importePagado,
            BigDecimal importeRecibido,
            BigDecimal vuelto) {
        this.venta = venta;
        this.medioPago = medioPago;
        this.importePagado = importePagado;
        this.importeRecibido = importeRecibido;
        this.vuelto = vuelto;
    }

    public Long getId() { return id; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public MedioPago getMedioPago() { return medioPago; }
    public void setMedioPago(MedioPago medioPago) { this.medioPago = medioPago; }
    public BigDecimal getImportePagado() { return importePagado; }
    public void setImportePagado(BigDecimal importePagado) { this.importePagado = importePagado; }
    public BigDecimal getImporteRecibido() { return importeRecibido; }
    public void setImporteRecibido(BigDecimal importeRecibido) { this.importeRecibido = importeRecibido; }
    public BigDecimal getVuelto() { return vuelto; }
    public void setVuelto(BigDecimal vuelto) { this.vuelto = vuelto; }
    public LocalDateTime getPagadoEn() { return pagadoEn; }
}
