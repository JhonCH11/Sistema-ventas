package com.minimarket.usuarios.service;

import com.minimarket.usuarios.domain.RolUsuario;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.form.ColaboradorForm;
import com.minimarket.usuarios.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ColaboradorService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    public ColaboradorService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios; this.encoder = encoder;
    }
    @Transactional(readOnly = true)
    public List<Usuario> listar() { return usuarios.findByRolOrderByNombreAsc(RolUsuario.VENDEDOR); }

    @Transactional
    public void crear(ColaboradorForm form) {
        validarDatos(form);
        validarContrasena(form);
        if (usuarios.existsByNombreUsuarioIgnoreCase(form.getNombreUsuario())) {
            throw new IllegalArgumentException("Ese usuario ya existe, incluso si esta desactivado.");
        }
        usuarios.saveAndFlush(new Usuario(form.getNombre().strip(), form.getNombreUsuario(),
                encoder.encode(form.getContrasena()), RolUsuario.VENDEDOR));
    }

    @Transactional
    public void editar(Long id, ColaboradorForm form) {
        var usuario = colaborador(id);
        validarDatos(form);
        if (usuarios.existsByNombreUsuarioIgnoreCaseAndIdNot(form.getNombreUsuario(), id)) {
            throw new IllegalArgumentException("Ese usuario ya existe, incluso si esta desactivado.");
        }
        usuario.setNombre(form.getNombre().strip());
        usuario.setNombreUsuario(form.getNombreUsuario());
        usuarios.flush();
    }

    private void validarDatos(ColaboradorForm form) {
        if (form.getNombre() == null || form.getNombre().isBlank() || form.getNombre().length() > 120
                || form.getNombreUsuario() == null || !form.getNombreUsuario().matches("[a-zA-Z0-9._-]{4,60}")) {
            throw new IllegalArgumentException("Revise el nombre y el usuario del colaborador.");
        }
    }
    @Transactional
    public void cambiarEstado(Long id, boolean activo) { colaborador(id).setActivo(activo); }

    @Transactional
    public void cambiarContrasena(Long id, ColaboradorForm form) {
        validarContrasena(form);
        colaborador(id).setContrasenaHash(encoder.encode(form.getContrasena()));
    }
    @Transactional(readOnly = true)
    public Usuario obtener(Long id) { return colaborador(id); }

    private Usuario colaborador(Long id) {
        return usuarios.findById(id).filter(u -> u.getRol() == RolUsuario.VENDEDOR)
                .orElseThrow(() -> new IllegalArgumentException("Colaborador no encontrado."));
    }
    private void validarContrasena(ColaboradorForm form) {
        String clave = form.getContrasena();
        if (clave == null || clave.length() < 8 || clave.getBytes(StandardCharsets.UTF_8).length > 72 || clave.isBlank()) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 8 caracteres y como maximo 72 bytes.");
        }
        if (!clave.equals(form.getConfirmarContrasena())) {
            throw new IllegalArgumentException("Las contrasenas no coinciden.");
        }
    }
}
