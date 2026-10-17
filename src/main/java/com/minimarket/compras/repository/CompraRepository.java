package com.minimarket.compras.repository;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompraRepository {
    private final JdbcTemplate jdbc;
    public CompraRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Compra(Long id,LocalDateTime fecha,String proveedor,String documento,String responsable,String observacion,BigDecimal total,boolean completa) {}
    public record Detalle(String producto,String tipo,BigDecimal cantidad,BigDecimal costo,BigDecimal subtotal) {}
    private static final String CONSULTA="""
        SELECT i.id,i.recibido_en,COALESCE(i.proveedor_nombre,p.nombre,'Sin proveedor') proveedor,
        i.documento,u.nombre responsable,i.observacion,i.total_compra,
        COALESCE(i.total_compra,(SELECT SUM(ROUND(d.cantidad*d.costo_unitario,2)) FROM ingreso_detalles d WHERE d.ingreso_mercaderia_id=i.id),0) total
        FROM ingresos_mercaderia i LEFT JOIN proveedores p ON p.id=i.proveedor_id JOIN usuarios u ON u.id=i.usuario_id
        """;
    private Compra fila(java.sql.ResultSet r,int n) throws java.sql.SQLException {
        return new Compra(r.getLong("id"),r.getTimestamp("recibido_en").toLocalDateTime(),r.getString("proveedor"),r.getString("documento"),r.getString("responsable"),r.getString("observacion"),r.getBigDecimal("total"),r.getBigDecimal("total_compra")!=null);
    }
    public List<Compra> listar(LocalDate desde,LocalDate hasta){return jdbc.query(CONSULTA+" WHERE i.recibido_en>=? AND i.recibido_en<? ORDER BY i.id DESC LIMIT 300",this::fila,desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay());}
    public Optional<Compra> obtener(Long id){return jdbc.query(CONSULTA+" WHERE i.id=?",this::fila,id).stream().findFirst();}
    public List<Detalle> detalles(Long id){return jdbc.query("""
        SELECT COALESCE(d.producto_nombre,p.nombre),COALESCE(d.tipo_venta,p.tipo_venta),d.cantidad,d.costo_unitario,
        ROUND(d.cantidad*d.costo_unitario,2) subtotal FROM ingreso_detalles d JOIN productos p ON p.id=d.producto_id WHERE d.ingreso_mercaderia_id=? ORDER BY d.id
        """,(r,n)->new Detalle(r.getString(1),r.getString(2),r.getBigDecimal(3),r.getBigDecimal(4),r.getBigDecimal(5)),id);}
    public Optional<Long> registrada(String token,Long usuarioId){return jdbc.query("SELECT id FROM ingresos_mercaderia WHERE token_compra=? AND usuario_id=?",(r,n)->r.getLong(1),token,usuarioId).stream().findFirst();}
}
