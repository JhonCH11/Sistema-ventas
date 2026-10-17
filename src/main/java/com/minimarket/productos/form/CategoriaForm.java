package com.minimarket.productos.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoriaForm {

    @NotBlank(message = "Ingresa el nombre de la categoría.")
    @Size(max = 100, message = "La categoría no puede superar 100 caracteres.")
    private String nombre;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
