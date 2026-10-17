package com.minimarket.stock.repository;

import com.minimarket.productos.domain.Producto;
import com.minimarket.stock.form.*;
import java.math.BigDecimal;
import java.sql.Statement;
import java.time.*;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class LiquidacionMermaRepository {
    private final JdbcTemplate jdbc;
    public LiquidacionMermaRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public boolean existeLiquidacion(String token){return jdbc.queryForObject("SELECT COUNT(*) FROM liquidaciones_stock WHERE token=?", Long.class, token)>0;}
    public boolean existeMerma(String token){return jdbc.queryForObject("SELECT COUNT(*) FROM mermas_stock WHERE token=?", Long.class, token)>0;}
    public long crearLiquidacion(LiquidacionForm form, Long usuario, LocalDateTime ahora) {
        var clave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps=con.prepareStatement("INSERT INTO liquidaciones_stock(token,producto_origen_id,usuario_id,cantidad_inicial,costo_unitario,fecha_llegada,motivo,registrada_en) VALUES(?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,form.getToken()); ps.setLong(2,form.getProductoId()); ps.setLong(3,usuario);
            ps.setBigDecimal(4,form.getCantidad()); ps.setBigDecimal(5,form.getCostoUnitario());
            ps.setObject(6,form.getFechaLlegada()); ps.setString(7,form.getMotivo().strip()); ps.setObject(8,ahora); return ps;
        }, clave);
        return java.util.Objects.requireNonNull(clave.getKey()).longValue();
    }
    public void vincular(long id, Long producto){jdbc.update("UPDATE liquidaciones_stock SET producto_liquidacion_id=? WHERE id=?",producto,id);}
    public BigDecimal costoLiquidacion(Long id){
        return jdbc.query("SELECT costo_unitario FROM liquidaciones_stock WHERE producto_liquidacion_id=?", rs -> rs.next()?rs.getBigDecimal(1):null,id);
    }
    public BigDecimal costoReciente(Long id){
        return jdbc.query("SELECT costo_unitario FROM ingreso_detalles WHERE producto_id=? ORDER BY id DESC LIMIT 1",rs -> rs.next()?rs.getBigDecimal(1):null,id);
    }
    public long crearMerma(MermaForm form, Producto producto, Long usuario, BigDecimal costo, BigDecimal perdida, LocalDateTime ahora){
        String base=producto.isLiquidacion()?jdbc.queryForObject("SELECT p.nombre FROM liquidaciones_stock l JOIN productos p ON p.id=l.producto_origen_id WHERE l.producto_liquidacion_id=?",String.class,producto.getId()):producto.getNombre();
        Object[] valores={form.getToken(),producto.getId(),usuario,producto.getNombre(),base,producto.getTipoVenta().name(),form.getCantidad(),costo,perdida,form.getMotivo().name(),form.getObservacion(),ahora};
        var clave=new GeneratedKeyHolder();
        jdbc.update(con->{
            var ps=con.prepareStatement("INSERT INTO mermas_stock(token,producto_id,usuario_id,producto_nombre,producto_base_nombre,tipo_venta,cantidad,costo_unitario,perdida,motivo,observacion,registrada_en) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
            for(int i=0;i<valores.length;i++)ps.setObject(i+1,valores[i]);return ps;
        },clave);
        return java.util.Objects.requireNonNull(clave.getKey()).longValue();
    }
    public List<Saldo> saldos(){
        return jdbc.query("SELECT l.id,p.id producto_id,p.nombre,p.tipo_venta,p.stock_actual,p.precio_actual,l.costo_unitario,l.fecha_llegada,l.registrada_en FROM liquidaciones_stock l JOIN productos p ON p.id=l.producto_liquidacion_id WHERE p.stock_actual>0 ORDER BY l.id",
                (rs,n)->new Saldo(rs.getLong("id"),rs.getLong("producto_id"),rs.getString("nombre"),rs.getString("tipo_venta"),rs.getBigDecimal("stock_actual"),rs.getBigDecimal("precio_actual"),rs.getBigDecimal("costo_unitario"),rs.getObject("fecha_llegada",LocalDate.class),rs.getObject("registrada_en",LocalDateTime.class)));
    }
    public List<Merma> mermas(LocalDate desde,LocalDate hasta){
        return jdbc.query("SELECT m.*,u.nombre administrador FROM mermas_stock m JOIN usuarios u ON u.id=m.usuario_id WHERE registrada_en>=? AND registrada_en<? ORDER BY m.id DESC",
                (rs,n)->new Merma(rs.getString("producto_nombre"),rs.getString("producto_base_nombre"),rs.getString("tipo_venta"),rs.getBigDecimal("cantidad"),rs.getBigDecimal("costo_unitario"),rs.getBigDecimal("perdida"),rs.getString("motivo"),rs.getString("observacion"),rs.getString("administrador"),rs.getObject("registrada_en",LocalDateTime.class)),desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay());
    }
    public record Saldo(long id,long productoId,String nombre,String tipo,BigDecimal cantidad,BigDecimal precio,BigDecimal costo,LocalDate llegada,LocalDateTime registrada){}
    public record Merma(String nombre,String base,String tipo,BigDecimal cantidad,BigDecimal costo,BigDecimal perdida,String motivo,String observacion,String administrador,LocalDateTime fecha){}
}
