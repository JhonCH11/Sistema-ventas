package com.minimarket.stock.form;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class MermaForm {
    public enum Motivo { DETERIORO, VENCIMIENTO, DANO, PERDIDA }
    @NotNull private Long productoId;
    @NotBlank private String token;
    @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) private BigDecimal cantidad;
    @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) private BigDecimal costoUnitario;
    @NotNull private Motivo motivo;
    @Size(max=200) private String observacion;
    public Long getProductoId(){return productoId;} public void setProductoId(Long v){productoId=v;}
    public String getToken(){return token;} public void setToken(String v){token=v;}
    public BigDecimal getCantidad(){return cantidad;} public void setCantidad(BigDecimal v){cantidad=v;}
    public BigDecimal getCostoUnitario(){return costoUnitario;} public void setCostoUnitario(BigDecimal v){costoUnitario=v;}
    public Motivo getMotivo(){return motivo;} public void setMotivo(Motivo v){motivo=v;}
    public String getObservacion(){return observacion;} public void setObservacion(String v){observacion=v;}
}
