package com.minimarket.caja;

import com.minimarket.caja.repository.HistorialCajaRepository;
import com.minimarket.caja.service.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HistorialCajaServiceTest {
    @Test void rechazaFechasInvertidasYSuperioresAlLimite() {
        var fecha=LocalDate.of(2026,10,16);
        assertThrows(IllegalArgumentException.class,()->HistorialCajaService.validarFechas(fecha,fecha.minusDays(1)));
        assertThrows(IllegalArgumentException.class,()->HistorialCajaService.validarFechas(fecha,fecha.plusDays(366)));
        assertDoesNotThrow(()->HistorialCajaService.validarFechas(fecha,fecha.plusDays(365)));
        assertDoesNotThrow(()->HistorialCajaService.validarFechas(fecha,fecha));
    }
    @Test void filtraAntesDeConsultar() {
        var repo=mock(HistorialCajaRepository.class);var service=new HistorialCajaService(repo,mock(MovimientoCajaService.class));
        assertThrows(IllegalArgumentException.class,()->service.listar(null,LocalDate.now()));verifyNoInteractions(repo);
    }
    @Test void detalleNoExistenteNoConsultaOtrosRegistros() {
        var repo=mock(HistorialCajaRepository.class);var movimientos=mock(MovimientoCajaService.class);
        when(repo.buscar(9L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,()->new HistorialCajaService(repo,movimientos).detalle(9L));
        verifyNoInteractions(movimientos);verify(repo,never()).cierres(any());
    }
}
