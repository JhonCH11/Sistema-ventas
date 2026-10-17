package com.minimarket.ventas.domain;

import com.minimarket.caja.domain.AperturaCaja;
import com.minimarket.usuarios.domain.Usuario;
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
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apertura_caja_id", nullable = false)
    private AperturaCaja aperturaCaja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @Column(name = "numero_ticket", nullable = false, unique = true, length = 30)
    private String numeroTicket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVenta estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "vendida_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime vendidaEn;

    protected Venta() {
    }

    public Venta(AperturaCaja aperturaCaja, Usuario vendedor, String numeroTicket, BigDecimal total) {
        this.aperturaCaja = aperturaCaja;
        this.vendedor = vendedor;
        this.numeroTicket = numeroTicket;
        this.estado = EstadoVenta.CONFIRMADA;
        this.total = total;
    }

    public Long getId() { return id; }
    public AperturaCaja getAperturaCaja() { return aperturaCaja; }
    public void setAperturaCaja(AperturaCaja aperturaCaja) { this.aperturaCaja = aperturaCaja; }
    public Usuario getVendedor() { return vendedor; }
    public void setVendedor(Usuario vendedor) { this.vendedor = vendedor; }
    public String getNumeroTicket() { return numeroTicket; }
    public void setNumeroTicket(String numeroTicket) { this.numeroTicket = numeroTicket; }
    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public LocalDateTime getVendidaEn() { return vendidaEn; }
}
