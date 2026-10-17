package com.minimarket.caja.domain;

import com.minimarket.usuarios.domain.Usuario;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "cierres_caja")
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apertura_caja_id", nullable = false)
    private AperturaCaja aperturaCaja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "efectivo_esperado", nullable = false, precision = 12, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(name = "efectivo_contado", nullable = false, precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(length = 300)
    private String observacion;

    @Column(name = "cerrada_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime cerradaEn;

    @Column(nullable = false)
    private boolean vigente = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reabierta_por")
    private Usuario reabiertaPor;

    @Column(name = "motivo_reapertura", length = 200)
    private String motivoReapertura;

    @Column(name = "reabierta_en")
    private LocalDateTime reabiertaEn;

    public void registrarReapertura(Usuario administrador, String motivo, LocalDateTime fecha) {
        vigente = false;
        reabiertaPor = administrador;
        motivoReapertura = motivo;
        reabiertaEn = fecha;
    }

    public boolean isVigente() { return vigente; }
    public String getMotivoReapertura() { return motivoReapertura; }

    protected CierreCaja() {
    }

    public CierreCaja(
            AperturaCaja aperturaCaja,
            Usuario usuario,
            BigDecimal efectivoEsperado,
            BigDecimal efectivoContado,
            String observacion) {
        this.aperturaCaja = aperturaCaja;
        this.usuario = usuario;
        this.efectivoEsperado = efectivoEsperado;
        this.efectivoContado = efectivoContado;
        this.diferencia = efectivoContado.subtract(efectivoEsperado);
        this.observacion = observacion;
    }

    public Long getId() { return id; }
    public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public BigDecimal getDiferencia() { return diferencia; }
    public String getObservacion() { return observacion; }
}
