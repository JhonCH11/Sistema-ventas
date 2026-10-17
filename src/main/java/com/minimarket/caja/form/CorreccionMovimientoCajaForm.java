package com.minimarket.caja.form;

import jakarta.validation.constraints.*;

public class CorreccionMovimientoCajaForm {
    @NotNull private Long aperturaId;
    @NotBlank private String token;
    @NotBlank(message = "Indique el motivo de la correccion.") @Size(max = 200) private String motivo;
    public Long getAperturaId() { return aperturaId; } public void setAperturaId(Long v) { aperturaId = v; }
    public String getToken() { return token; } public void setToken(String v) { token = v; }
    public String getMotivo() { return motivo; } public void setMotivo(String v) { motivo = v; }
}
