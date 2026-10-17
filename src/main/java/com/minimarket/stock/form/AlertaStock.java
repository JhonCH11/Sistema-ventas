package com.minimarket.stock.form;

import com.minimarket.productos.domain.TipoVenta;
import java.math.BigDecimal;

public record AlertaStock(Long id, String nombre, TipoVenta tipoVenta, BigDecimal cantidad, BigDecimal minimo) {
    public boolean isAgotado() { return cantidad.signum() == 0; }
    public String getUnidad() { return tipoVenta == TipoVenta.PESO ? "kg" : "unidades"; }
}
