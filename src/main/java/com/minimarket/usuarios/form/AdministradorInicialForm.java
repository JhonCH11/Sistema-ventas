package com.minimarket.usuarios.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdministradorInicialForm {

    @NotBlank(message = "Ingresa el nombre del administrador.")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
    private String nombre;

    @NotBlank(message = "Ingresa un nombre de usuario.")
    @Size(min = 4, max = 60, message = "El usuario debe tener entre 4 y 60 caracteres.")
    private String nombreUsuario;

    @NotBlank(message = "Ingresa una contraseña.")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
    private String contrasena;

    @NotBlank(message = "Confirma la contraseña.")
    private String confirmarContrasena;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public String getConfirmarContrasena() { return confirmarContrasena; }
    public void setConfirmarContrasena(String confirmarContrasena) {
        this.confirmarContrasena = confirmarContrasena;
    }
}
