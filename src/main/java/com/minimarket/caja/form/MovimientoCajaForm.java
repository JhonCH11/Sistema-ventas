package com.minimarket.caja.form;

import com.minimarket.caja.domain.TipoMovimientoCaja;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class MovimientoCajaForm {
    @NotNull private Long aperturaId;
    @NotBlank private String token;
    @NotNull(message = "Seleccione el tipo de movimiento.") private TipoMovimientoCaja tipo;
    @NotNull(message = "Ingrese el importe.")
    @DecimalMin(value = "0.01", message = "El importe debe ser mayor a cero.")
    @Digits(integer = 10, fraction = 2, message = "Use hasta dos decimales y diez cifras enteras.")
    private BigDecimal importe;
    @NotBlank(message = "Indique el motivo.")
    @Size(max = 200, message = "El motivo admite hasta 200 caracteres.")
    private String motivo;
    public Long getAperturaId() { return aperturaId; } public void setAperturaId(Long v) { aperturaId = v; }
    public String getToken() { return token; } public void setToken(String v) { token = v; }
    public TipoMovimientoCaja getTipo() { return tipo; } public void setTipo(TipoMovimientoCaja v) { tipo = v; }
    public BigDecimal getImporte() { return importe; } public void setImporte(BigDecimal v) { importe = v; }
    public String getMotivo() { return motivo; } public void setMotivo(String v) { motivo = v; }
}
