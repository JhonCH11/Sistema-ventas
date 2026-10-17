package com.minimarket.caja.service;

import com.minimarket.caja.domain.EstadoCaja;
import com.minimarket.caja.form.ExtensionHorarioForm;
import com.minimarket.caja.repository.AperturaCajaRepository;
import com.minimarket.caja.repository.HorarioCajaRepository;
import com.minimarket.usuarios.domain.RolUsuario;
import com.minimarket.usuarios.service.UsuarioService;
import java.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HorarioCajaService {
    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private final AperturaCajaRepository aperturas;
    private final HorarioCajaRepository horarios;
    private final UsuarioService usuarios;
    private final Clock reloj;

    @Autowired
    public HorarioCajaService(AperturaCajaRepository aperturas, HorarioCajaRepository horarios, UsuarioService usuarios) {
        this(aperturas, horarios, usuarios, Clock.system(ZONA));
    }

    public HorarioCajaService(AperturaCajaRepository aperturas, HorarioCajaRepository horarios, UsuarioService usuarios, Clock reloj) {
        this.aperturas = aperturas; this.horarios = horarios; this.usuarios = usuarios; this.reloj = reloj.withZone(ZONA);
    }

    @Transactional(readOnly = true)
    public Horario consultar(String nombreUsuario) {
        var ahora = ZonedDateTime.now(reloj);
        var apertura = aperturas.findByFechaOperacion(ahora.toLocalDate());
        LocalTime cierre = apertura.map(a -> horarios.cierrePrevisto(a.getId())).orElse(LocalTime.of(21, 0));
        String estado = apertura.map(a -> a.getEstado().name()).orElse("SIN_ABRIR");
        boolean administrador = usuarios.obtenerUsuarioActivo(nombreUsuario).getRol() == RolUsuario.ADMINISTRADOR;
        return new Horario(ahora.toInstant().toEpochMilli(), ahora.toLocalDate().toString(),
                ahora.toLocalDate().atTime(6, 0).atZone(ZONA).toInstant().toEpochMilli(),
                ahora.toLocalDate().atTime(cierre).atZone(ZONA).toInstant().toEpochMilli(),
                cierre.toString(), estado, administrador);
    }

    @Transactional
    public void extender(ExtensionHorarioForm form, String nombreUsuario) {
        var usuario = usuarios.obtenerUsuarioActivo(nombreUsuario);
        if (usuario.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new IllegalArgumentException("Solo el administrador puede extender el horario.");
        }
        var ahora = ZonedDateTime.now(reloj);
        if (!ahora.toLocalDate().equals(form.getFecha())) {
            throw new IllegalArgumentException("La fecha cambio. Vuelva a abrir la extension de horario.");
        }
        var apertura = aperturas.bloquearPorFecha(ahora.toLocalDate())
                .filter(a -> a.getEstado() == EstadoCaja.ABIERTA)
                .orElseThrow(() -> new IllegalArgumentException("Debe existir una caja abierta para hoy."));
        LocalTime anterior = horarios.cierrePrevisto(apertura.getId());
        if (form.getNuevaHora() == null || !form.getNuevaHora().isAfter(anterior)
                || !form.getNuevaHora().isAfter(ahora.toLocalTime())
                || form.getNuevaHora().isAfter(LocalTime.of(23, 59))
                || form.getNuevaHora().getSecond() != 0 || form.getNuevaHora().getNano() != 0) {
            throw new IllegalArgumentException("Elija una hora posterior al cierre previsto y a la hora actual, hasta las 23:59 de hoy.");
        }
        if (form.getMotivo() == null || form.getMotivo().isBlank() || form.getMotivo().length() > 200) {
            throw new IllegalArgumentException("Indique un motivo de hasta 200 caracteres.");
        }
        horarios.registrar(apertura.getId(), usuario.getId(), anterior, form.getNuevaHora(), form.getMotivo().strip());
    }

    public record Horario(long ahora, String fecha, long apertura, long cierre, String horaCierre,
                          String estado, boolean administrador) {}
}
