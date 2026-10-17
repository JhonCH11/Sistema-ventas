package com.minimarket.importaciones.form;

import com.minimarket.productos.domain.TipoVenta;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ImportacionExcel {
    private ImportacionExcel() {}
    public enum Tipo { PRODUCTOS, STOCK }
    public record Fila(int numero,String nombre,String categoria,TipoVenta tipo,BigDecimal precio,BigDecimal minimo,
                       BigDecimal cantidad,BigDecimal costo,String observacion,Long productoId,Long categoriaId,List<String> errores) implements Serializable {}
    public record Previa(String token,Tipo tipo,String referencia,String hash,Long usuarioId,Instant creada,List<Fila> filas) implements Serializable {
        public boolean isValida() {return !filas.isEmpty()&&filas.stream().allMatch(f->f.errores().isEmpty());}
    }
}
