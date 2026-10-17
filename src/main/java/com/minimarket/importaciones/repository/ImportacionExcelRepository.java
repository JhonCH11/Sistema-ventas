package com.minimarket.importaciones.repository;

import com.minimarket.importaciones.form.ImportacionExcel.Previa;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ImportacionExcelRepository {
    private final JdbcTemplate jdbc;
    public ImportacionExcelRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public boolean registrada(String token,Long usuario) {return jdbc.queryForObject("SELECT COUNT(*) FROM importaciones_excel WHERE token=? AND usuario_id=?",Long.class,token,usuario)>0;}
    public boolean duplicada(String tipo,String referencia,String hash) {return jdbc.queryForObject("SELECT COUNT(*) FROM importaciones_excel WHERE tipo=? AND (referencia=? OR sha256=?)",Long.class,tipo,referencia,hash)>0;}
    public void registrar(Previa previa) {
        jdbc.update("INSERT INTO importaciones_excel(token,tipo,referencia,sha256,usuario_id,filas,importada_en) VALUES(?,?,?,?,?,?,?)",previa.token(),previa.tipo().name(),previa.referencia(),previa.hash(),previa.usuarioId(),previa.filas().size(),LocalDateTime.now(ZoneId.of("America/Lima")));
    }
}
