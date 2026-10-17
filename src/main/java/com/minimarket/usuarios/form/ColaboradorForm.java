package com.minimarket.usuarios.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ColaboradorForm {
    @NotBlank(message = "Ingrese el nombre.")
    @Size(max = 120)
    private String nombre;
    @NotBlank(message = "Ingrese el usuario.")
    @Pattern(regexp = "[a-zA-Z0-9._-]{4,60}", message = "Use de 4 a 60 letras, numeros, puntos, guiones o guiones bajos.")
    private String nombreUsuario;
    private String contrasena;
    private String confirmarContrasena;
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public String getConfirmarContrasena() { return confirmarContrasena; }
    public void setConfirmarContrasena(String confirmarContrasena) { this.confirmarContrasena = confirmarContrasena; }
}
