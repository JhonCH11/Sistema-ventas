package com.minimarket.caja;

import com.minimarket.caja.domain.*;
import com.minimarket.caja.form.ReaperturaCajaForm;
import com.minimarket.caja.repository.*;
import com.minimarket.caja.service.ReaperturaCajaService;
import com.minimarket.usuarios.domain.*;
import com.minimarket.usuarios.service.UsuarioService;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReaperturaCajaServiceTest {
    private final AperturaCajaRepository aperturas = mock(AperturaCajaRepository.class);
    private final CierreCajaRepository cierres = mock(CierreCajaRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final AperturaCaja apertura = mock(AperturaCaja.class);
    private final CierreCaja cierre = mock(CierreCaja.class);
    private final Usuario admin = mock(Usuario.class);
    private final ReaperturaCajaService servicio = new ReaperturaCajaService(aperturas, cierres, usuarios,
            Clock.fixed(Instant.parse("2026-10-10T01:00:00Z"), ZoneOffset.UTC));
    private ReaperturaCajaForm preparar() {
        when(usuarios.obtenerUsuarioActivo("admin")).thenReturn(admin);
        when(admin.getRol()).thenReturn(RolUsuario.ADMINISTRADOR);
        when(aperturas.bloquearPorFecha(LocalDate.of(2026, 10, 9))).thenReturn(Optional.of(apertura));
        when(apertura.getId()).thenReturn(1L);
        when(apertura.getEstado()).thenReturn(EstadoCaja.CERRADA);
        when(cierre.getId()).thenReturn(3L);
        when(cierres.findByAperturaCajaIdAndVigenteTrue(1L)).thenReturn(Optional.of(cierre));
        var form = new ReaperturaCajaForm(); form.setFecha(LocalDate.of(2026, 10, 9));
        form.setCierreId(3L); form.setMotivo(" Clientes pendientes "); return form;
    }
    @Test void reabreConAuditoriaSinEliminarCierre() {
        servicio.reabrir(preparar(), "admin");
        verify(cierre).registrarReapertura(admin, "Clientes pendientes", LocalDateTime.of(2026, 10, 9, 20, 0));
        verify(apertura).setEstado(EstadoCaja.ABIERTA);
        verify(cierres, never()).delete(any());
    }
    @Test void rechazaVendedor() {
        var form = preparar(); when(admin.getRol()).thenReturn(RolUsuario.VENDEDOR);
        assertThrows(IllegalArgumentException.class, () -> servicio.reabrir(form, "admin"));
        verifyNoInteractions(aperturas);
    }
    @Test void rechazaDiaAnterior() {
        var form = preparar(); form.setFecha(form.getFecha().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> servicio.reabrir(form, "admin"));
        verifyNoInteractions(aperturas);
    }
    @Test void rechazaReintentoCuandoYaEstaAbierta() {
        var form = preparar(); when(apertura.getEstado()).thenReturn(EstadoCaja.ABIERTA);
        assertThrows(IllegalArgumentException.class, () -> servicio.reabrir(form, "admin"));
        verify(cierre, never()).registrarReapertura(any(), any(), any());
    }
    @Test void rechazaFormularioDeUnCierreAnterior() {
        var form = preparar(); form.setCierreId(2L);
        assertThrows(IllegalArgumentException.class, () -> servicio.reabrir(form, "admin"));
        verify(apertura, never()).setEstado(any());
    }
    @Test void exigeMotivo() {
        var form = preparar(); form.setMotivo(" ");
        assertThrows(IllegalArgumentException.class, () -> servicio.reabrir(form, "admin"));
        verifyNoInteractions(aperturas);
    }
    @Test void conservaImportesDelCierreHistorico() {
        var anterior = new CierreCaja(apertura, admin, new java.math.BigDecimal("200.00"), new java.math.BigDecimal("199.00"), "Diferencia");
        anterior.registrarReapertura(admin, "Continuar ventas", LocalDateTime.of(2026, 10, 9, 20, 0));
        assertFalse(anterior.isVigente());
        assertEquals(new java.math.BigDecimal("199.00"), anterior.getEfectivoContado());
        assertEquals(new java.math.BigDecimal("-1.00"), anterior.getDiferencia());
    }
}
