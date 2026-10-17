package com.minimarket.caja.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

public class ExtensionHorarioForm {
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;
    @NotNull(message = "Indique la nueva hora de cierre.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime nuevaHora;
    @NotBlank(message = "Indique el motivo de la extension.")
    @Size(max = 200, message = "El motivo admite hasta 200 caracteres.")
    private String motivo;

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getNuevaHora() { return nuevaHora; }
    public void setNuevaHora(LocalTime nuevaHora) { this.nuevaHora = nuevaHora; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
