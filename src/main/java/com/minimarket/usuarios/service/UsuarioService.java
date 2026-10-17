package com.minimarket.usuarios.service;

import com.minimarket.usuarios.domain.RolUsuario;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.form.AdministradorInicialForm;
import com.minimarket.usuarios.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean existeAlgunUsuario() {
        return usuarioRepository.count() > 0;
    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioActivo(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuarioAndActivoTrue(nombreUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario activo no fue encontrado."));
    }

    @Transactional
    public void registrarAdministradorInicial(AdministradorInicialForm formulario) {
        String nombreUsuario = formulario.getNombreUsuario().trim();
        if (usuarioRepository.existsByNombreUsuarioIgnoreCase(nombreUsuario)) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está en uso.");
        }

        Usuario administrador = new Usuario(
                formulario.getNombre().trim(),
                nombreUsuario,
                passwordEncoder.encode(formulario.getContrasena()),
                RolUsuario.ADMINISTRADOR);
        administrador.setActivo(true);
        usuarioRepository.save(administrador);
    }
}
