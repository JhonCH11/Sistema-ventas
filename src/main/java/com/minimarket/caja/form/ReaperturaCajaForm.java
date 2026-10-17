package com.minimarket.caja.form;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class ReaperturaCajaForm {
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;
    @NotNull
    private Long cierreId;
    @NotBlank(message = "Indique por que necesita reabrir la caja.")
    @Size(max = 200, message = "El motivo admite hasta 200 caracteres.")
    private String motivo;
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Long getCierreId() { return cierreId; }
    public void setCierreId(Long cierreId) { this.cierreId = cierreId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
