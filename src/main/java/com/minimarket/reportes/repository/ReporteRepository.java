package com.minimarket.reportes.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.minimarket.ventas.domain.MedioPago;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReporteRepository {
    private final JdbcTemplate jdbc;
    public ReporteRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public List<FilaVenta> ventas(LocalDateTime inicio,LocalDateTime fin) {
        return jdbc.query("""
                SELECT v.numero_ticket,v.vendida_en,v.estado,v.total,p.medio_pago,
                       COALESCE(r.devuelto,0) devuelto
                FROM ventas v JOIN pagos p ON p.venta_id=v.id
                LEFT JOIN (
                  SELECT d.venta_id,SUM(dd.importe_devuelto) devuelto
                  FROM devoluciones d JOIN devolucion_detalles dd ON dd.devolucion_id=d.id
                  GROUP BY d.venta_id
                ) r ON r.venta_id=v.id
                WHERE v.vendida_en>=? AND v.vendida_en<?
                ORDER BY v.vendida_en DESC,v.id DESC
                """, (rs,n)->new FilaVenta(rs.getString("numero_ticket"),
                        rs.getTimestamp("vendida_en").toLocalDateTime(),
                        rs.getString("estado"),rs.getBigDecimal("total"),
                        MedioPago.valueOf(rs.getString("medio_pago")),rs.getBigDecimal("devuelto")),
                inicio,fin);
    }

    public List<Reembolso> reembolsos(LocalDateTime inicio,LocalDateTime fin) {
        return jdbc.query("""
                SELECT p.medio_pago,COALESCE(SUM(dd.importe_devuelto),0) importe
                FROM devoluciones d JOIN devolucion_detalles dd ON dd.devolucion_id=d.id
                JOIN pagos p ON p.venta_id=d.venta_id
                WHERE d.devuelta_en>=? AND d.devuelta_en<?
                GROUP BY p.medio_pago
                """,(rs,n)->new Reembolso(MedioPago.valueOf(rs.getString("medio_pago")),
                        rs.getBigDecimal("importe")),inicio,fin);
    }

    public record FilaVenta(String numero,LocalDateTime fecha,String estado,BigDecimal total,
            MedioPago medio,BigDecimal devuelto) {
        public BigDecimal neto() {
            return estado.equals("ANULADA") ? BigDecimal.ZERO : total.subtract(devuelto);
        }
    }
    public record Reembolso(MedioPago medio,BigDecimal importe) {}
}

