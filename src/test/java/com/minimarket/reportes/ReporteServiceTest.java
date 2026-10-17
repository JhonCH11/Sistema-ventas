package com.minimarket.reportes;
import com.minimarket.reportes.service.ReporteService;
import com.minimarket.reportes.repository.ReporteRepository;
import com.minimarket.reportes.repository.ReporteRepository.*;
import com.minimarket.ventas.domain.MedioPago;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReporteServiceTest {
    @Test void descuentaAnulacionesYDevolucionesSinMezclarReembolsosDeOtrasFechas() {
        var fecha=LocalDateTime.of(2026,10,5,12,0);
        var ventas=List.of(new FilaVenta("1",fecha,"CONFIRMADA",new BigDecimal("100.00"),MedioPago.EFECTIVO,new BigDecimal("20.00")),
                new FilaVenta("2",fecha,"ANULADA",new BigDecimal("30.00"),MedioPago.YAPE,BigDecimal.ZERO));
        var resultado=ReporteService.resumir(ventas,List.of(new Reembolso(MedioPago.EFECTIVO,new BigDecimal("50.00"))));
        assertEquals(new BigDecimal("80.00"),resultado.neto());
        assertEquals(new BigDecimal("30.00"),resultado.anulado());
        assertEquals(new BigDecimal("50.00"),resultado.medios().get(0).reembolsosPeriodo());
    }
    @Test void sinVentasTodosLosMediosSonCero() {
        var resultado=ReporteService.resumir(List.of(),List.of());
        assertEquals(0,resultado.neto().signum());
        assertEquals(3,resultado.medios().size());
    }
    @Test void rechazaFechasInvertidasSinConsultarBase() {
        var repo=mock(ReporteRepository.class);
        var service=new ReporteService(repo);
        assertThrows(IllegalArgumentException.class,()->service.consultar(LocalDate.of(2026,10,5),LocalDate.of(2026,10,4)));
        verifyNoInteractions(repo);
    }
}

