package com.minimarket.stock.domain;

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
import java.time.LocalDateTime;

@Entity
@Table(name = "ingresos_mercaderia")
public class IngresoMercaderia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 300)
    private String observacion;

    @Column(name="proveedor_id")
    private Long proveedorId;
    @Column(name="proveedor_nombre",length=150)
    private String proveedorNombre;
    @Column(name="token_compra",length=36)
    private String tokenCompra;
    @Column(length=100)
    private String documento;
    @Column(name="total_compra",precision=12,scale=2)
    private java.math.BigDecimal totalCompra;

    @Column(name = "recibido_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime recibidoEn;

    protected IngresoMercaderia() {
    }

    public IngresoMercaderia(Usuario usuario, String observacion) {
        this.usuario = usuario;
        this.observacion = observacion;
    }

    public Long getId() { return id; }
    public void datosCompra(Long proveedorId,String proveedorNombre,String token,String documento,java.math.BigDecimal total) {
        this.proveedorId=proveedorId;this.proveedorNombre=proveedorNombre;this.tokenCompra=token;this.documento=documento;this.totalCompra=total;
    }
    public Usuario getUsuario() { return usuario; }
    public String getObservacion() { return observacion; }
    public LocalDateTime getRecibidoEn() { return recibidoEn; }
}
