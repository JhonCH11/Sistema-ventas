package com.minimarket.seguridad;

import com.minimarket.usuarios.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class UsuarioActivoFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;
    public UsuarioActivoFilter(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && usuarios.findByNombreUsuarioAndActivoTrue(auth.getName()).filter(usuario ->
                    !(auth.getPrincipal() instanceof com.minimarket.usuarios.service.UsuarioAutenticacionService.UsuarioSesion sesionUsuario)
                    || java.util.Objects.equals(usuario.getId(), sesionUsuario.getUsuarioId())).isEmpty()) {
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext();
            response.sendRedirect(request.getContextPath() + "/ingresar?inactivo");
            return;
        }
        chain.doFilter(request, response);
    }
}
