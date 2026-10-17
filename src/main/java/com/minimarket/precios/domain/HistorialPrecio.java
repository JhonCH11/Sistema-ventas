package com.minimarket.precios.domain;

import com.minimarket.productos.domain.Producto;
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
@Table(name = "historial_precios")
public class HistorialPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "precio_anterior", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioAnterior;

    @Column(name = "precio_nuevo", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioNuevo;

    @Column(name = "cambiado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime cambiadoEn;

    protected HistorialPrecio() {
    }

    public HistorialPrecio(Producto producto, Usuario usuario, BigDecimal precioAnterior, BigDecimal precioNuevo) {
        this.producto = producto;
        this.usuario = usuario;
        this.precioAnterior = precioAnterior;
        this.precioNuevo = precioNuevo;
    }

    public Long getId() { return id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public BigDecimal getPrecioAnterior() { return precioAnterior; }
    public void setPrecioAnterior(BigDecimal precioAnterior) { this.precioAnterior = precioAnterior; }
    public BigDecimal getPrecioNuevo() { return precioNuevo; }
    public void setPrecioNuevo(BigDecimal precioNuevo) { this.precioNuevo = precioNuevo; }
    public LocalDateTime getCambiadoEn() { return cambiadoEn; }
}
