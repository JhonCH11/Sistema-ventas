package com.minimarket.usuarios;

import com.minimarket.seguridad.UsuarioActivoFilter;
import com.minimarket.usuarios.domain.*;
import com.minimarket.usuarios.repository.UsuarioRepository;
import com.minimarket.usuarios.service.UsuarioAutenticacionService.UsuarioSesion;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentidadSesionTest {
    @Test void usuarioReutilizadoNoHeredaLaSesionAnterior() throws Exception {
        var original = mock(Usuario.class);
        when(original.getId()).thenReturn(1L);
        when(original.getNombreUsuario()).thenReturn("anterior");
        when(original.getContrasenaHash()).thenReturn("hash");
        when(original.getRol()).thenReturn(RolUsuario.VENDEDOR);
        var principal = new UsuarioSesion(original);
        var repo = mock(UsuarioRepository.class);
        var otro = mock(Usuario.class);
        when(otro.getId()).thenReturn(2L);
        when(repo.findByNombreUsuarioAndActivoTrue("anterior")).thenReturn(Optional.of(otro));
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
        try {
            new UsuarioActivoFilter(repo).doFilter(request, response, chain);
            assertEquals("/ingresar?inactivo", response.getRedirectedUrl());
            verifyNoInteractions(chain);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        } finally { SecurityContextHolder.clearContext(); }
    }
}
