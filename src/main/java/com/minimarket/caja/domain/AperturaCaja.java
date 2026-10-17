package com.minimarket.caja.domain;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "aperturas_caja")
public class AperturaCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_operacion", nullable = false)
    private LocalDate fechaOperacion;

    @Column(name = "efectivo_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal efectivoInicial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCaja estado;

    @Column(name = "abierta_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime abiertaEn;

    protected AperturaCaja() {
    }

    public AperturaCaja(Usuario usuario, LocalDate fechaOperacion, BigDecimal efectivoInicial) {
        this.usuario = usuario;
        this.fechaOperacion = fechaOperacion;
        this.efectivoInicial = efectivoInicial;
        this.estado = EstadoCaja.ABIERTA;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDate getFechaOperacion() { return fechaOperacion; }
    public void setFechaOperacion(LocalDate fechaOperacion) { this.fechaOperacion = fechaOperacion; }
    public BigDecimal getEfectivoInicial() { return efectivoInicial; }
    public void setEfectivoInicial(BigDecimal efectivoInicial) { this.efectivoInicial = efectivoInicial; }
    public EstadoCaja getEstado() { return estado; }
    public void setEstado(EstadoCaja estado) { this.estado = estado; }
    public LocalDateTime getAbiertaEn() { return abiertaEn; }
}
