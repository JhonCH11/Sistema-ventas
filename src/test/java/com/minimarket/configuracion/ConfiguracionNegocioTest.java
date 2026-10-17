package com.minimarket.configuracion;

import com.minimarket.configuracion.domain.ConfiguracionNegocio;
import com.minimarket.configuracion.form.ConfiguracionNegocioForm;
import com.minimarket.configuracion.repository.ConfiguracionNegocioRepository;
import com.minimarket.configuracion.service.ConfiguracionNegocioService;
import jakarta.validation.Validation;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguracionNegocioTest {
    @Test
    void validaNombreYLimitesDeCampos() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var form = new ConfiguracionNegocioForm();
            form.setNombre("   ");
            assertFalse(validator.validate(form).isEmpty());
            form.setNombre("Minimarket Flavia");
            assertTrue(validator.validate(form).isEmpty());
            form.setDireccion("a".repeat(201));
            form.setTelefono("1".repeat(31));
            assertEquals(2, validator.validate(form).size());
        }
    }

    @Test
    void guardaDatosYCarpetaDeRespaldos() {
        var repository = mock(ConfiguracionNegocioRepository.class);
        var negocio = mock(ConfiguracionNegocio.class);
        when(repository.findById((byte) 1)).thenReturn(Optional.of(negocio));
        var form = new ConfiguracionNegocioForm();
        form.setNombre("  Flavia  ");
        form.setDireccion("  Calle principal  ");
        String ruta = java.nio.file.Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().toString();
        form.setRutaRespaldos(ruta);
        new ConfiguracionNegocioService(repository).guardar(form);
        verify(negocio).setNombre("Flavia");
        verify(negocio).setDireccion("Calle principal");
        verify(negocio).setTelefono(null);
        verify(negocio).setRutaRespaldos(ruta);
        verify(repository).save(negocio);
    }

    @Test
    void noCreaOtraConfiguracionSiFaltaLaInicial() {
        var repository = mock(ConfiguracionNegocioRepository.class);
        when(repository.findById((byte) 1)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class,
                () -> new ConfiguracionNegocioService(repository).obtener());
        verify(repository, never()).save(any());
    }
}
