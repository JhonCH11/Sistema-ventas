package com.minimarket.ventas.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CorreccionVentaRepository {
    private final JdbcTemplate jdbc;
    public CorreccionVentaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public BigDecimal cantidadDevuelta(Long detalle) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(cantidad_devuelta),0) FROM devolucion_detalles WHERE venta_detalle_id=?", BigDecimal.class, detalle);
    }
    public BigDecimal importeDevuelto(Long detalle) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(importe_devuelto),0) FROM devolucion_detalles WHERE venta_detalle_id=?", BigDecimal.class, detalle);
    }
    public boolean tieneDevoluciones(Long venta) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM devoluciones WHERE venta_id=?", Integer.class, venta) > 0;
    }
    public boolean existeToken(String token) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM devoluciones WHERE token_operacion=?", Integer.class, token) > 0;
    }
    public void anular(Long venta, Long admin, String motivo, String observacion) {
        jdbc.update("INSERT INTO anulaciones_venta(venta_id,administrador_id,motivo,observacion) VALUES(?,?,?,?)", venta, admin, motivo, observacion);
    }
    public void devolver(Long venta, Long admin, Long caja, String token, String motivo,
            String observacion, Long detalle, BigDecimal cantidad, BigDecimal importe, boolean repone) {
        jdbc.update("INSERT INTO devoluciones(venta_id,administrador_id,apertura_caja_id,token_operacion,motivo,observacion) VALUES(?,?,?,?,?,?)",
                venta, admin, caja, token, motivo, observacion);
        Long id = jdbc.queryForObject("SELECT id FROM devoluciones WHERE token_operacion=?", Long.class, token);
        jdbc.update("INSERT INTO devolucion_detalles(devolucion_id,venta_detalle_id,cantidad_devuelta,importe_devuelto,repone_stock) VALUES(?,?,?,?,?)",
                id, detalle, cantidad, importe, repone);
    }
    public BigDecimal reembolsosEfectivo(Long caja) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(dd.importe_devuelto),0)
                FROM devoluciones d JOIN devolucion_detalles dd ON dd.devolucion_id=d.id
                JOIN pagos p ON p.venta_id=d.venta_id
                WHERE d.apertura_caja_id=? AND p.medio_pago='EFECTIVO'
                """, BigDecimal.class, caja);
    }
    public BigDecimal totalDevuelto(Long venta) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(dd.importe_devuelto),0) FROM devoluciones d JOIN devolucion_detalles dd ON dd.devolucion_id=d.id WHERE d.venta_id=?", BigDecimal.class, venta);
    }
    public List<Map<String,Object>> historial(Long venta) {
        return jdbc.queryForList("""
                SELECT d.devuelta_en fecha,d.motivo,dd.cantidad_devuelta cantidad,
                dd.importe_devuelto importe,dd.repone_stock,vd.producto_nombre producto
                FROM devoluciones d JOIN devolucion_detalles dd ON dd.devolucion_id=d.id
                JOIN venta_detalles vd ON vd.id=dd.venta_detalle_id
                WHERE d.venta_id=? ORDER BY d.id DESC
                """, venta);
    }
}

