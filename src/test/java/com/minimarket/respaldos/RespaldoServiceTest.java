package com.minimarket.respaldos;

import com.minimarket.configuracion.domain.ConfiguracionNegocio;
import com.minimarket.configuracion.service.ConfiguracionNegocioService;
import com.minimarket.respaldos.service.EjecutorRespaldo;
import com.minimarket.respaldos.service.RespaldoService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RespaldoServiceTest {
    @TempDir Path carpeta;

    private RespaldoService service(EjecutorRespaldo ejecutor, String url) {
        var configuracion = mock(ConfiguracionNegocioService.class);
        var negocio = mock(ConfiguracionNegocio.class);
        when(configuracion.obtener()).thenReturn(negocio);
        when(negocio.getRutaRespaldos()).thenReturn(carpeta.toString());
        return new RespaldoService(configuracion, ejecutor, url, "root", "clave-secreta", "mysqldump");
    }

    @Test
    void generaCopiasUnicasYLimpiaCredenciales() throws Exception {
        var ejecutor = mock(EjecutorRespaldo.class);
        List<Path> temporales = new ArrayList<>();
        doAnswer(invocacion -> {
            List<String> comando = invocacion.getArgument(0);
            assertFalse(comando.stream().anyMatch(arg -> arg.contains("clave-secreta")));
            Path credenciales = Path.of(comando.get(1).substring("--defaults-extra-file=".length()));
            assertTrue(Files.readString(credenciales).contains("password=\"clave-secreta\""));
            temporales.add(credenciales);
            Files.writeString(invocacion.getArgument(1), "CREATE TABLE ejemplo(id INT);");
            return null;
        }).when(ejecutor).ejecutar(anyList(), any());
        var service = service(ejecutor, "jdbc:mysql://localhost:3306/minimarket_db?serverTimezone=America/Lima");
        Path primera = service.generar();
        Path segunda = service.generar();
        assertNotEquals(primera, segunda);
        assertTrue(Files.size(primera) > 0);
        assertTrue(primera.toString().endsWith(".sql"));
        temporales.forEach(path -> assertFalse(Files.exists(path)));
        try (var archivos = Files.list(carpeta)) {
            assertEquals(2, archivos.count());
        }
    }

    @Test
    void falloNoDejaCopiaParcialNiClaveEnMensaje() throws Exception {
        var ejecutor = mock(EjecutorRespaldo.class);
        doThrow(new IOException("clave-secreta")).when(ejecutor).ejecutar(anyList(), any());
        var ex = assertThrows(IllegalStateException.class,
                () -> service(ejecutor, "jdbc:mysql://localhost/minimarket_db").generar());
        assertFalse(ex.getMessage().contains("clave-secreta"));
        try (var archivos = Files.list(carpeta)) { assertEquals(0, archivos.count()); }
    }

    @Test
    void rechazaRespaldoVacio() {
        assertThrows(IllegalStateException.class,
                () -> service(mock(EjecutorRespaldo.class), "jdbc:mysql://localhost/minimarket_db").generar());
    }

    @Test
    void noConectaAUnServidorRemoto() {
        var ejecutor = mock(EjecutorRespaldo.class);
        assertThrows(IllegalStateException.class,
                () -> service(ejecutor, "jdbc:mysql://servidor.example/minimarket_db").generar());
        verifyNoInteractions(ejecutor);
    }

    @Test
    void falloDeRespaldoNoRepiteNiOcultaElCierre() {
        var caja = mock(com.minimarket.caja.service.CajaService.class);
        var respaldo = mock(RespaldoService.class);
        when(respaldo.generar()).thenThrow(new IllegalStateException("Respaldo fallido."));
        var controller = new com.minimarket.caja.controller.CajaController(caja, respaldo);
        var formulario = new com.minimarket.caja.form.CierreCajaForm();
        var authentication = mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        var atributos = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        String vista = controller.cerrarCaja(formulario,
                mock(org.springframework.validation.BindingResult.class), authentication,
                new org.springframework.ui.ExtendedModelMap(), atributos);
        assertEquals("redirect:/caja", vista);
        var orden = inOrder(caja, respaldo);
        orden.verify(caja).cerrarCaja(formulario, "admin");
        orden.verify(respaldo).generar();
        assertTrue(atributos.getFlashAttributes().get("errorGeneral").toString().contains("quedo cerrada"));
    }
}
