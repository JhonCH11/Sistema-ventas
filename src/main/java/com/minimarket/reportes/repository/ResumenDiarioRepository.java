package com.minimarket.reportes.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ResumenDiarioRepository {
    private final JdbcTemplate jdbc;
    public ResumenDiarioRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public BigDecimal mermas(LocalDate fecha) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(perdida),0) FROM mermas_stock WHERE registrada_en>=? AND registrada_en<?",
                BigDecimal.class,fecha.atStartOfDay(),fecha.plusDays(1).atStartOfDay());
    }
    public Map<String,BigDecimal> movimientos(Long aperturaId) {
        return jdbc.query("SELECT tipo,SUM(impacto) importe FROM movimientos_dinero_caja WHERE apertura_caja_id=? GROUP BY tipo",
                (rs,n)->Map.entry(rs.getString("tipo"),rs.getBigDecimal("importe")),aperturaId).stream().collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }
}
