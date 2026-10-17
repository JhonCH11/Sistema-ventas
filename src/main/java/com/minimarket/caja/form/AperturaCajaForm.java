package com.minimarket.caja.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AperturaCajaForm {

    @NotNull(message = "Ingresa el efectivo inicial.")
    @DecimalMin(value = "0.00", message = "El efectivo inicial no puede ser negativo.")
    private BigDecimal efectivoInicial;

    public BigDecimal getEfectivoInicial() { return efectivoInicial; }
    public void setEfectivoInicial(BigDecimal efectivoInicial) { this.efectivoInicial = efectivoInicial; }
}
