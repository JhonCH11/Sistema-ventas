package com.minimarket.stock.form;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public class AjusteStockForm {
    public enum Motivo { CONTEO_FISICO, PRODUCTO_VENCIDO, PRODUCTO_DANADO, PERDIDA }
    @NotNull private Motivo motivo;
    @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) private BigDecimal cantidad;
    @NotNull private BigDecimal stockEsperado;
    @Size(max=300) private String observacion;
    public Motivo getMotivo(){return motivo;}
    public void setMotivo(Motivo v){motivo=v;}
    public BigDecimal getCantidad(){return cantidad;}
    public void setCantidad(BigDecimal v){cantidad=v;}
    public BigDecimal getStockEsperado(){return stockEsperado;}
    public void setStockEsperado(BigDecimal v){stockEsperado=v;}
    public String getObservacion(){return observacion;}
    public void setObservacion(String v){observacion=v;}
}

