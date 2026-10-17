package com.minimarket.reportes.service;

import com.minimarket.caja.service.CajaService;
import com.minimarket.caja.repository.AperturaCajaRepository;
import com.minimarket.reportes.repository.ResumenDiarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ResumenDiarioService {
    private final ReporteService reportes;
    private final ResumenDiarioRepository datos;
    private final AperturaCajaRepository aperturas;
    private final CajaService caja;
    public ResumenDiarioService(ReporteService reportes,ResumenDiarioRepository datos,AperturaCajaRepository aperturas,CajaService caja) {
        this.reportes=reportes;this.datos=datos;this.aperturas=aperturas;this.caja=caja;
    }
    @Transactional(readOnly=true)
    public Resumen consultar(LocalDate fecha) {
        if(fecha==null||fecha.getYear()<1000||fecha.getYear()>9998)throw new IllegalArgumentException("Seleccione una fecha valida.");
        var reporte=reportes.consultar(fecha,fecha);
        var apertura=aperturas.findByFechaOperacion(fecha);
        var cierre=apertura.flatMap(caja::buscarCierre);
        Map<String,BigDecimal> movimientos=apertura.map(a->datos.movimientos(a.getId())).orElse(Map.of());
        var reembolsos=reporte.medios().stream().map(ReporteService.ResumenMedio::reembolsosPeriodo).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new Resumen(fecha,reporte,reporte.bruto().subtract(reporte.anulado()),reembolsos,datos.mermas(fecha),
                apertura.map(a->a.getEstado().name()).orElse("SIN_ABRIR"),
                apertura.map(a->a.getEfectivoInicial()).orElse(null),
                cierre.map(c->c.getEfectivoEsperado()).orElseGet(()->apertura.map(caja::calcularEfectivoEsperado).orElse(null)),
                cierre.map(c->c.getEfectivoContado()).orElse(null),cierre.map(c->c.getDiferencia()).orElse(null),
                movimientos.getOrDefault("INGRESO",BigDecimal.ZERO),movimientos.getOrDefault("RETIRO",BigDecimal.ZERO).negate(),movimientos.getOrDefault("GASTO",BigDecimal.ZERO).negate());
    }
    public record Resumen(LocalDate fecha,ReporteService.Reporte ventas,BigDecimal cobros,BigDecimal reembolsos,BigDecimal mermas,
                          String estado,BigDecimal inicial,BigDecimal esperado,BigDecimal contado,BigDecimal diferencia,
                          BigDecimal ingresos,BigDecimal retiros,BigDecimal gastos) {}
}
