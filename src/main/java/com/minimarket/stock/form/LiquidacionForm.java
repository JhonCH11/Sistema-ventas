package com.minimarket.stock.form;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class LiquidacionForm {
    @NotNull private Long productoId;
    @NotBlank private String token;
    @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) private BigDecimal cantidad;
    @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) private BigDecimal precio;
    @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) private BigDecimal costoUnitario;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate fechaLlegada;
    @NotBlank @Size(max=200) private String motivo;
    public Long getProductoId(){return productoId;} public void setProductoId(Long v){productoId=v;}
    public String getToken(){return token;} public void setToken(String v){token=v;}
    public BigDecimal getCantidad(){return cantidad;} public void setCantidad(BigDecimal v){cantidad=v;}
    public BigDecimal getPrecio(){return precio;} public void setPrecio(BigDecimal v){precio=v;}
    public BigDecimal getCostoUnitario(){return costoUnitario;} public void setCostoUnitario(BigDecimal v){costoUnitario=v;}
    public LocalDate getFechaLlegada(){return fechaLlegada;} public void setFechaLlegada(LocalDate v){fechaLlegada=v;}
    public String getMotivo(){return motivo;} public void setMotivo(String v){motivo=v;}
}
