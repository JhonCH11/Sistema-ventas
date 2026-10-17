package com.minimarket.inicio;

import com.minimarket.usuarios.domain.RolUsuario;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.ui.ExtendedModelMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InicioControllerTest {
    @Test void muestraNombreRegistradoEnLugarDelUsuarioDeAcceso() {
        var usuarios = mock(UsuarioService.class);
        when(usuarios.obtenerUsuarioActivo("ana01")).thenReturn(new Usuario("Ana Maria Perez", "ana01", "hash", RolUsuario.VENDEDOR));
        var auth = new UsernamePasswordAuthenticationToken("ana01", "", AuthorityUtils.createAuthorityList("ROLE_VENDEDOR"));
        var model = new ExtendedModelMap();
        assertEquals("inicio", new InicioController(usuarios, mock(com.minimarket.stock.service.AlertaStockService.class)).mostrarInicio(model, auth));
        assertEquals("Ana Maria Perez", model.get("nombrePersona"));
        assertEquals(false, model.get("administrador"));
        assertFalse(model.containsAttribute("nombreUsuario"));
    }

    @Test void conservaOpcionesAdministrativasAlMostrarElNombre() {
        var usuarios = mock(UsuarioService.class);
        when(usuarios.obtenerUsuarioActivo("admin")).thenReturn(new Usuario("Flavia", "admin", "hash", RolUsuario.ADMINISTRADOR));
        var auth = new UsernamePasswordAuthenticationToken("admin", "", AuthorityUtils.createAuthorityList("ROLE_ADMINISTRADOR"));
        var model = new ExtendedModelMap();
        new InicioController(usuarios, mock(com.minimarket.stock.service.AlertaStockService.class)).mostrarInicio(model, auth);
        assertEquals("Flavia", model.get("nombrePersona"));
        assertEquals(true, model.get("administrador"));
    }
}
