package com.minimarket.caja;

import com.minimarket.caja.domain.AperturaCaja;
import com.minimarket.caja.domain.EstadoCaja;
import com.minimarket.caja.form.ExtensionHorarioForm;
import com.minimarket.caja.repository.AperturaCajaRepository;
import com.minimarket.caja.repository.HorarioCajaRepository;
import com.minimarket.caja.service.HorarioCajaService;
import com.minimarket.usuarios.domain.RolUsuario;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.service.UsuarioService;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HorarioCajaServiceTest {
    private final AperturaCajaRepository aperturas = mock(AperturaCajaRepository.class);
    private final HorarioCajaRepository horarios = mock(HorarioCajaRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final Usuario admin = mock(Usuario.class);
    private final AperturaCaja caja = mock(AperturaCaja.class);
    private final LocalDate fecha = LocalDate.of(2026, 10, 8);
    private final HorarioCajaService servicio = new HorarioCajaService(aperturas, horarios, usuarios,
            Clock.fixed(Instant.parse("2026-10-09T01:45:00Z"), ZoneOffset.UTC));

    private ExtensionHorarioForm preparar() {
        when(usuarios.obtenerUsuarioActivo("admin")).thenReturn(admin);
        when(admin.getRol()).thenReturn(RolUsuario.ADMINISTRADOR);
        when(admin.getId()).thenReturn(7L);
        when(caja.getId()).thenReturn(1L);
        when(caja.getEstado()).thenReturn(EstadoCaja.ABIERTA);
        when(aperturas.bloquearPorFecha(fecha)).thenReturn(Optional.of(caja));
        when(horarios.cierrePrevisto(1L)).thenReturn(LocalTime.of(21, 0));
        var form = new ExtensionHorarioForm();
        form.setFecha(fecha);
        form.setNuevaHora(LocalTime.of(22, 0));
        form.setMotivo(" Clientes pendientes ");
        return form;
    }

    @Test void registraExtensionConAdministradorYMotivo() {
        servicio.extender(preparar(), "admin");
        verify(horarios).registrar(1L, 7L, LocalTime.of(21, 0), LocalTime.of(22, 0), "Clientes pendientes");
    }

    @Test void rechazaVendedor() {
        var form = preparar();
        when(admin.getRol()).thenReturn(RolUsuario.VENDEDOR);
        assertThrows(IllegalArgumentException.class, () -> servicio.extender(form, "admin"));
        verifyNoInteractions(horarios);
    }

    @Test void rechazaHoraAnteriorAlCierre() {
        var form = preparar();
        form.setNuevaHora(LocalTime.of(20, 50));
        assertThrows(IllegalArgumentException.class, () -> servicio.extender(form, "admin"));
        verify(horarios, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test void rechazaCajaCerrada() {
        var form = preparar();
        when(caja.getEstado()).thenReturn(EstadoCaja.CERRADA);
        assertThrows(IllegalArgumentException.class, () -> servicio.extender(form, "admin"));
        verifyNoInteractions(horarios);
    }

    @Test void rechazaFormularioDeOtroDia() {
        var form = preparar();
        form.setFecha(fecha.minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> servicio.extender(form, "admin"));
        verifyNoInteractions(horarios);
    }

    @Test void consultaHorarioConFechaPeruAunqueUtcSeaDiaSiguiente() {
        preparar();
        when(aperturas.findByFechaOperacion(fecha)).thenReturn(Optional.of(caja));
        var horario = servicio.consultar("admin");
        assertEquals("2026-10-08", horario.fecha());
        assertEquals(Instant.parse("2026-10-08T11:00:00Z").toEpochMilli(), horario.apertura());
        assertEquals(Instant.parse("2026-10-09T02:00:00Z").toEpochMilli(), horario.cierre());
        assertTrue(horario.administrador());
    }
}
