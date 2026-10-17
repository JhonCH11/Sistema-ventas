package com.minimarket.reportes.service;

import com.minimarket.reportes.repository.ReporteRepository;
import com.minimarket.reportes.repository.ReporteRepository.FilaVenta;
import com.minimarket.reportes.repository.ReporteRepository.Reembolso;
import com.minimarket.ventas.domain.MedioPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteService {
    private final ReporteRepository repositorio;
    public ReporteService(ReporteRepository repositorio) { this.repositorio=repositorio; }

    @Transactional(readOnly=true)
    public Reporte consultar(LocalDate desde,LocalDate hasta) {
        if(desde==null || hasta==null || hasta.isBefore(desde))
            throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial.");
        var ventas=repositorio.ventas(desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay());
        var reembolsos=repositorio.reembolsos(desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay());
        return resumir(ventas,reembolsos);
    }

    public static Reporte resumir(List<FilaVenta> ventas,List<Reembolso> reembolsos) {
        BigDecimal bruto=ventas.stream().map(FilaVenta::total).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal anulado=ventas.stream().filter(v->v.estado().equals("ANULADA"))
                .map(FilaVenta::total).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal devuelto=ventas.stream().filter(v->!v.estado().equals("ANULADA"))
                .map(FilaVenta::devuelto).reduce(BigDecimal.ZERO,BigDecimal::add);
        var medios=java.util.Arrays.stream(MedioPago.values()).map(medio-> {
            BigDecimal cobrado=ventas.stream().filter(v->v.medio()==medio && !v.estado().equals("ANULADA"))
                    .map(FilaVenta::total).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal devoluciones=ventas.stream().filter(v->v.medio()==medio && !v.estado().equals("ANULADA"))
                    .map(FilaVenta::devuelto).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal reembolso=reembolsos.stream().filter(r->r.medio()==medio)
                    .map(Reembolso::importe).reduce(BigDecimal.ZERO,BigDecimal::add);
            return new ResumenMedio(medio,cobrado,devoluciones,cobrado.subtract(devoluciones),reembolso);
        }).toList();
        return new Reporte(ventas,bruto,anulado,devuelto,bruto.subtract(anulado).subtract(devuelto),medios);
    }

    public record Reporte(List<FilaVenta> ventas,BigDecimal bruto,BigDecimal anulado,
            BigDecimal devuelto,BigDecimal neto,List<ResumenMedio> medios) {}
    public record ResumenMedio(MedioPago medio,BigDecimal cobrado,BigDecimal devoluciones,
            BigDecimal neto,BigDecimal reembolsosPeriodo) {}
}

