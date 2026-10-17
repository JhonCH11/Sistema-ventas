package com.minimarket.caja.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CierreCajaForm {

    @NotNull(message = "Ingresa el efectivo contado.")
    @DecimalMin(value = "0.00", message = "El efectivo contado no puede ser negativo.")
    private BigDecimal efectivoContado;

    @Size(max = 300, message = "La observación no puede superar 300 caracteres.")
    private String observacion;

    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(BigDecimal efectivoContado) { this.efectivoContado = efectivoContado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
