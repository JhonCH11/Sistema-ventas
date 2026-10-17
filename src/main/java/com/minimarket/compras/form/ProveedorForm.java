package com.minimarket.compras.form;

import jakarta.validation.constraints.*;

public class ProveedorForm {
    @NotBlank(message="Ingrese el nombre del proveedor.") @Size(max=150)
    private String nombre;
    @Size(max=30,message="El telefono permite hasta 30 caracteres.")
    private String telefono;
    private boolean activo=true;
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getTelefono(){return telefono;} public void setTelefono(String v){telefono=v;}
    public boolean isActivo(){return activo;} public void setActivo(boolean v){activo=v;}
}
