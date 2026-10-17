package com.minimarket.usuarios.service;

import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioAutenticacionService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAutenticacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuarioAndActivoTrue(nombreUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));

        return new UsuarioSesion(usuario);
    }

    public static class UsuarioSesion extends User {
        private final Long usuarioId;
        public UsuarioSesion(Usuario usuario) {
            super(usuario.getNombreUsuario(), usuario.getContrasenaHash(),
                    org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_" + usuario.getRol().name()));
            usuarioId = usuario.getId();
        }
        public Long getUsuarioId() { return usuarioId; }
    }
}
