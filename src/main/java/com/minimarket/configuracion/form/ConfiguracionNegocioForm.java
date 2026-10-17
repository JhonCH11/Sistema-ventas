package com.minimarket.configuracion.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConfiguracionNegocioForm {
    @NotBlank(message = "Ingrese el nombre del minimarket.")
    @Size(max = 120, message = "El nombre admite hasta 120 caracteres.")
    private String nombre;

    @Size(max = 200, message = "La direccion admite hasta 200 caracteres.")
    private String direccion;

    @Size(max = 30, message = "El telefono admite hasta 30 caracteres.")
    private String telefono;

    @Size(max = 500, message = "La ruta admite hasta 500 caracteres.")
    private String rutaRespaldos;

    public String getRutaRespaldos() { return rutaRespaldos; }
    public void setRutaRespaldos(String rutaRespaldos) { this.rutaRespaldos = rutaRespaldos; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
