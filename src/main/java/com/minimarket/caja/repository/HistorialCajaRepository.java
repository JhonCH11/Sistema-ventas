package com.minimarket.caja.repository;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class HistorialCajaRepository {
    private final JdbcTemplate jdbc;
    public HistorialCajaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String JORNADA = """
            SELECT a.id,a.fecha_operacion,a.abierta_en,a.efectivo_inicial,a.estado,u.nombre responsable,
                   c.efectivo_esperado,c.efectivo_contado,c.diferencia,
                   (SELECT COUNT(*) FROM cierres_caja r WHERE r.apertura_caja_id=a.id AND r.reabierta_en IS NOT NULL) reaperturas
            FROM aperturas_caja a JOIN usuarios u ON u.id=a.usuario_id
            LEFT JOIN cierres_caja c ON c.apertura_caja_id=a.id AND c.vigente=TRUE
            """;
    private static final RowMapper<Jornada> MAPA = (rs, n) -> new Jornada(rs.getLong("id"),
            rs.getObject("fecha_operacion", LocalDate.class), rs.getObject("abierta_en", LocalDateTime.class),
            rs.getString("responsable"), rs.getBigDecimal("efectivo_inicial"), rs.getString("estado"),
            rs.getBigDecimal("efectivo_esperado"), rs.getBigDecimal("efectivo_contado"), rs.getBigDecimal("diferencia"), rs.getInt("reaperturas"));

    public List<Jornada> listar(LocalDate desde, LocalDate hasta) {
        return jdbc.query(JORNADA + " WHERE a.fecha_operacion BETWEEN ? AND ? ORDER BY a.fecha_operacion DESC,a.id DESC", MAPA, desde, hasta);
    }
    public Optional<Jornada> buscar(Long id) {
        return jdbc.query(JORNADA + " WHERE a.id=?", MAPA, id).stream().findFirst();
    }
    public List<Cierre> cierres(Long id) {
        return jdbc.query("""
                SELECT c.*,u.nombre responsable,r.nombre administrador_reapertura
                FROM cierres_caja c JOIN usuarios u ON u.id=c.usuario_id
                LEFT JOIN usuarios r ON r.id=c.reabierta_por
                WHERE c.apertura_caja_id=? ORDER BY c.id DESC
                """, (rs,n)->new Cierre(rs.getLong("id"),rs.getObject("cerrada_en",LocalDateTime.class),rs.getString("responsable"),
                rs.getBigDecimal("efectivo_esperado"),rs.getBigDecimal("efectivo_contado"),rs.getBigDecimal("diferencia"),rs.getString("observacion"),
                rs.getBoolean("vigente"),rs.getObject("reabierta_en",LocalDateTime.class),rs.getString("administrador_reapertura"),rs.getString("motivo_reapertura")),id);
    }
    public List<Extension> extensiones(Long id) {
        return jdbc.query("""
                SELECT e.*,u.nombre responsable FROM extensiones_horario_caja e JOIN usuarios u ON u.id=e.usuario_id
                WHERE e.apertura_caja_id=? ORDER BY e.id DESC
                """,(rs,n)->new Extension(rs.getObject("autorizada_en",LocalDateTime.class),rs.getObject("hora_anterior",LocalTime.class),
                rs.getObject("hora_nueva",LocalTime.class),rs.getString("responsable"),rs.getString("motivo")),id);
    }
    public record Jornada(long id,LocalDate fecha,LocalDateTime abiertaEn,String responsable,BigDecimal inicial,String estado,
                          BigDecimal esperado,BigDecimal contado,BigDecimal diferencia,int reaperturas) {}
    public record Cierre(long id,LocalDateTime fecha,String responsable,BigDecimal esperado,BigDecimal contado,BigDecimal diferencia,
                         String observacion,boolean vigente,LocalDateTime reabiertaEn,String reabiertaPor,String motivoReapertura) {}
    public record Extension(LocalDateTime fecha,LocalTime anterior,LocalTime nueva,String responsable,String motivo) {}
}
