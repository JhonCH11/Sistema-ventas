package com.minimarket.caja.service;

import com.minimarket.caja.domain.MovimientoDineroCaja;
import com.minimarket.caja.repository.HistorialCajaRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class HistorialCajaService {
    private final HistorialCajaRepository historial;
    private final MovimientoCajaService movimientos;
    public HistorialCajaService(HistorialCajaRepository historial,MovimientoCajaService movimientos) { this.historial=historial;this.movimientos=movimientos; }
    @Transactional(readOnly=true)
    public List<HistorialCajaRepository.Jornada> listar(LocalDate desde,LocalDate hasta) {
        validarFechas(desde,hasta);return historial.listar(desde,hasta);
    }
    @Transactional(readOnly=true)
    public Detalle detalle(Long id) {
        var jornada=historial.buscar(id).orElseThrow(()->new IllegalArgumentException("No se encontro esa jornada de caja."));
        return new Detalle(jornada,historial.cierres(id),historial.extensiones(id),movimientos.listar(id));
    }
    public static void validarFechas(LocalDate desde,LocalDate hasta) {
        if(desde==null||hasta==null||desde.getYear()<1000||hasta.getYear()>9999||desde.isAfter(hasta))
            throw new IllegalArgumentException("Revise las fechas. Desde debe ser anterior o igual a Hasta.");
        if(ChronoUnit.DAYS.between(desde,hasta)>365)
            throw new IllegalArgumentException("Consulte hasta 366 dias por vez.");
    }
    public record Detalle(HistorialCajaRepository.Jornada jornada,List<HistorialCajaRepository.Cierre> cierres,
                          List<HistorialCajaRepository.Extension> extensiones,List<MovimientoDineroCaja> movimientos) {}
}
