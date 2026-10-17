package com.minimarket.caja.domain;

import com.minimarket.usuarios.domain.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_dinero_caja")
public class MovimientoDineroCaja {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apertura_caja_id", nullable = false)
    private AperturaCaja aperturaCaja;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(nullable = false, unique = true, length = 36, columnDefinition = "char(36)")
    private String token;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    private TipoMovimientoCaja tipo;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal impacto;
    @Column(nullable = false, length = 200)
    private String motivo;
    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversion_de_id", unique = true)
    private MovimientoDineroCaja reversionDe;

    protected MovimientoDineroCaja() {}
    public MovimientoDineroCaja(AperturaCaja apertura, Usuario usuario, String token, TipoMovimientoCaja tipo,
                               BigDecimal importe, BigDecimal impacto, String motivo, LocalDateTime fecha, MovimientoDineroCaja reversionDe) {
        this.aperturaCaja = apertura; this.usuario = usuario; this.token = token; this.tipo = tipo;
        this.importe = importe; this.impacto = impacto; this.motivo = motivo; this.registradoEn = fecha; this.reversionDe = reversionDe;
    }
    public Long getId() { return id; }
    public AperturaCaja getAperturaCaja() { return aperturaCaja; }
    public Usuario getUsuario() { return usuario; }
    public TipoMovimientoCaja getTipo() { return tipo; }
    public BigDecimal getImporte() { return importe; }
    public BigDecimal getImpacto() { return impacto; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getRegistradoEn() { return registradoEn; }
    public MovimientoDineroCaja getReversionDe() { return reversionDe; }
}
