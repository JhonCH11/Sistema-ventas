package com.minimarket.caja.domain;

public enum TipoMovimientoCaja {
    INGRESO("Ingreso adicional"), RETIRO("Retiro"), GASTO("Gasto");
    private final String descripcion;
    TipoMovimientoCaja(String descripcion) { this.descripcion = descripcion; }
    public String getDescripcion() { return descripcion; }
}
