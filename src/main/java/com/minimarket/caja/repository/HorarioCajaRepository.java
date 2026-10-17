package com.minimarket.caja.repository;

import java.time.LocalTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HorarioCajaRepository {
    private final JdbcTemplate jdbc;
    public HorarioCajaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public LocalTime cierrePrevisto(Long aperturaId) {
        return jdbc.query("SELECT MAX(hora_nueva) FROM extensiones_horario_caja WHERE apertura_caja_id=?",
                rs -> rs.next() && rs.getTime(1) != null ? rs.getTime(1).toLocalTime() : LocalTime.of(21, 0), aperturaId);
    }

    public void registrar(Long aperturaId, Long usuarioId, LocalTime anterior, LocalTime nueva, String motivo) {
        jdbc.update("INSERT INTO extensiones_horario_caja(apertura_caja_id,usuario_id,hora_anterior,hora_nueva,motivo) VALUES (?,?,?,?,?)",
                aperturaId, usuarioId, java.sql.Time.valueOf(anterior), java.sql.Time.valueOf(nueva), motivo);
    }
}
