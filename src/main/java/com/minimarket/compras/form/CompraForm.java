package com.minimarket.compras.form;

import com.minimarket.stock.form.IngresoStockForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

public class CompraForm {
    @NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}",message="Abra nuevamente Nueva compra.")
    private String token;
    private Long proveedorId;
    @Size(max=100,message="El documento permite hasta 100 caracteres.")
    private String documento;
    @Size(max=300,message="La observacion permite hasta 300 caracteres.")
    private String observacion;
    @NotEmpty @Size(max=50,message="Se permiten hasta 50 productos por compra.")
    private List<@Valid @NotNull IngresoStockForm> lineas=new ArrayList<>();
    public String getToken(){return token;} public void setToken(String v){token=v;}
    public Long getProveedorId(){return proveedorId;} public void setProveedorId(Long v){proveedorId=v;}
    public String getDocumento(){return documento;} public void setDocumento(String v){documento=v;}
    public String getObservacion(){return observacion;} public void setObservacion(String v){observacion=v;}
    public List<IngresoStockForm> getLineas(){return lineas;} public void setLineas(List<IngresoStockForm> v){lineas=v;}
}
