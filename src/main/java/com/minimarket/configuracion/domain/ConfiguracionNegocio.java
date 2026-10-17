package com.minimarket.configuracion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_negocio")
public class ConfiguracionNegocio {

    @Id
    private Byte id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 200)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Column(name = "ruta_respaldos", length = 500)
    private String rutaRespaldos;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    protected ConfiguracionNegocio() {
    }

    public Byte getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getRutaRespaldos() { return rutaRespaldos; }
    public void setRutaRespaldos(String rutaRespaldos) { this.rutaRespaldos = rutaRespaldos; }
}
