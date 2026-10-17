package com.minimarket.ventas.form;

import com.minimarket.ventas.domain.MedioPago;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ConfirmarVentaForm {

    @NotNull(message = "Selecciona un medio de pago.")
    private MedioPago medioPago;

    private BigDecimal importeRecibido;
    private boolean abonoVerificado;
    private String token;
    public boolean isAbonoVerificado() { return abonoVerificado; }
    public void setAbonoVerificado(boolean valor) { abonoVerificado = valor; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public MedioPago getMedioPago() { return medioPago; }
    public void setMedioPago(MedioPago medioPago) { this.medioPago = medioPago; }
    public BigDecimal getImporteRecibido() { return importeRecibido; }
    public void setImporteRecibido(BigDecimal importeRecibido) { this.importeRecibido = importeRecibido; }
}
