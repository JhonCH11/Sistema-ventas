package com.minimarket.caja;

import com.minimarket.caja.domain.AperturaCaja;
import com.minimarket.caja.repository.*;
import com.minimarket.caja.service.CajaService;
import com.minimarket.usuarios.service.UsuarioService;
import com.minimarket.ventas.repository.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EfectivoEsperadoTest {
    @Test void incluyeMovimientosSinModificarVentas() {
        var pagos=mock(PagoRepository.class);var correcciones=mock(CorreccionVentaRepository.class);var movimientos=mock(MovimientoDineroCajaRepository.class);
        var apertura=mock(AperturaCaja.class);when(apertura.getId()).thenReturn(1L);when(apertura.getEfectivoInicial()).thenReturn(new BigDecimal("100.00"));
        when(pagos.totalCobradoPorAperturaYMedioPago(eq(1L),any(),any())).thenReturn(new BigDecimal("50.00"));
        when(correcciones.reembolsosEfectivo(1L)).thenReturn(new BigDecimal("10.00"));
        when(movimientos.impactoTotal(1L)).thenReturn(new BigDecimal("25.00"));
        var service=new CajaService(mock(AperturaCajaRepository.class),mock(CierreCajaRepository.class),mock(UsuarioService.class),pagos,correcciones,movimientos);
        assertEquals(new BigDecimal("165.00"),service.calcularEfectivoEsperado(apertura));
    }
}
