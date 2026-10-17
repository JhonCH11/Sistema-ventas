package com.minimarket.usuarios;

import com.minimarket.usuarios.domain.*;
import com.minimarket.usuarios.form.ColaboradorForm;
import com.minimarket.usuarios.repository.UsuarioRepository;
import com.minimarket.usuarios.service.ColaboradorService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ColaboradorServiceTest {
    private final UsuarioRepository repo = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final ColaboradorService service = new ColaboradorService(repo, encoder);
    private ColaboradorForm form() {
        var form = new ColaboradorForm();
        form.setNombre(" Ana Perez "); form.setNombreUsuario("ana.perez");
        form.setContrasena("clave-prueba-123"); form.setConfirmarContrasena("clave-prueba-123");
        return form;
    }
    @Test void creaSoloVendedorConClaveCifrada() {
        service.crear(form());
        var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(repo).saveAndFlush(captor.capture());
        var usuario = captor.getValue();
        assertEquals("Ana Perez", usuario.getNombre());
        assertEquals(RolUsuario.VENDEDOR, usuario.getRol());
        assertTrue(usuario.isActivo());
        assertTrue(encoder.matches("clave-prueba-123", usuario.getContrasenaHash()));
    }
    @Test void rechazaUsuarioDuplicado() {
        when(repo.existsByNombreUsuarioIgnoreCase("ana.perez")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.crear(form()));
        verify(repo, never()).saveAndFlush(any());
    }
    @Test void rechazaClavesDistintas() {
        var form = form(); form.setConfirmarContrasena("otra-clave");
        assertThrows(IllegalArgumentException.class, () -> service.crear(form));
        verifyNoInteractions(repo);
    }
    @Test void rechazaClaveMultibyteMayorA72Bytes() {
        var form = form(); form.setContrasena("\u00e1".repeat(37));
        form.setConfirmarContrasena(form.getContrasena());
        assertThrows(IllegalArgumentException.class, () -> service.crear(form));
    }
    @Test void noPermiteDesactivarAdministrador() {
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(new Usuario("Admin", "admin", "hash", RolUsuario.ADMINISTRADOR)));
        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(1L, false));
    }
    @Test void desactivaSinBorrarUsuario() {
        var usuario = new Usuario("Ana", "ana.perez", "hash", RolUsuario.VENDEDOR);
        when(repo.findById(2L)).thenReturn(java.util.Optional.of(usuario));
        service.cambiarEstado(2L, false);
        assertFalse(usuario.isActivo());
        verify(repo, never()).delete(any());
        service.cambiarEstado(2L, true);
        assertTrue(usuario.isActivo());
    }
    @Test void cambiaClaveConHashSeguro() {
        var usuario = new Usuario("Ana", "ana.perez", "hash", RolUsuario.VENDEDOR);
        when(repo.findById(2L)).thenReturn(java.util.Optional.of(usuario));
        service.cambiarContrasena(2L, form());
        assertTrue(encoder.matches("clave-prueba-123", usuario.getContrasenaHash()));
    }

    @Test void editaDatosSinCambiarClaveRolNiEstado() {
        var usuario = new Usuario("Ana", "anterior", "hash-original", RolUsuario.VENDEDOR);
        usuario.setActivo(false);
        when(repo.findById(2L)).thenReturn(java.util.Optional.of(usuario));
        service.editar(2L, form());
        assertEquals("Ana Perez", usuario.getNombre());
        assertEquals("ana.perez", usuario.getNombreUsuario());
        assertEquals("hash-original", usuario.getContrasenaHash());
        assertEquals(RolUsuario.VENDEDOR, usuario.getRol());
        assertFalse(usuario.isActivo());
        verify(repo).existsByNombreUsuarioIgnoreCaseAndIdNot("ana.perez", 2L);
        verify(repo).flush();
    }

    @Test void noModificaDatosSiElUsuarioYaExiste() {
        var usuario = new Usuario("Ana", "anterior", "hash", RolUsuario.VENDEDOR);
        when(repo.findById(2L)).thenReturn(java.util.Optional.of(usuario));
        when(repo.existsByNombreUsuarioIgnoreCaseAndIdNot("ana.perez", 2L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.editar(2L, form()));
        assertEquals("anterior", usuario.getNombreUsuario());
        assertEquals("Ana", usuario.getNombre());
        verify(repo, never()).flush();
    }

    @Test void noPermiteEditarAdministrador() {
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(new Usuario("Admin", "admin", "hash", RolUsuario.ADMINISTRADOR)));
        assertThrows(IllegalArgumentException.class, () -> service.editar(1L, form()));
        verify(repo, never()).flush();
    }
}
