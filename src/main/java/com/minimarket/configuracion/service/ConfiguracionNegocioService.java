package com.minimarket.configuracion.service;

import com.minimarket.configuracion.domain.ConfiguracionNegocio;
import com.minimarket.configuracion.form.ConfiguracionNegocioForm;
import com.minimarket.configuracion.repository.ConfiguracionNegocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

@Service
@Validated
public class ConfiguracionNegocioService {
    private final ConfiguracionNegocioRepository repository;

    public ConfiguracionNegocioService(ConfiguracionNegocioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ConfiguracionNegocio obtener() {
        return repository.findById((byte) 1).orElseThrow(() ->
                new IllegalStateException("No se encontro la configuracion del negocio."));
    }

    @Transactional
    public void guardar(@Valid ConfiguracionNegocioForm formulario) {
        String ruta = limpiar(formulario.getRutaRespaldos());
        if (ruta != null && !ruta.isBlank()) {
            try {
                if (!java.nio.file.Path.of(ruta).isAbsolute()) {
                    throw new IllegalArgumentException("Ingrese una ruta absoluta para los respaldos.");
                }
            } catch (java.nio.file.InvalidPathException ex) {
                throw new IllegalArgumentException("La ruta de respaldos no es valida.");
            }
        }
        ConfiguracionNegocio configuracion = obtener();
        configuracion.setNombre(limpiar(formulario.getNombre()));
        configuracion.setDireccion(limpiar(formulario.getDireccion()));
        configuracion.setTelefono(limpiar(formulario.getTelefono()));
        configuracion.setRutaRespaldos(ruta == null || ruta.isBlank() ? null : ruta);
        repository.save(configuracion);
    }

    private String limpiar(String valor) {
        return valor == null ? null : valor.strip();
    }
}
