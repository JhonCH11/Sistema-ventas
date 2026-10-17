package com.minimarket.caja.service;

import com.minimarket.caja.domain.EstadoCaja;
import com.minimarket.caja.form.ReaperturaCajaForm;
import com.minimarket.caja.repository.*;
import com.minimarket.usuarios.domain.RolUsuario;
import com.minimarket.usuarios.service.UsuarioService;
import java.time.*;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReaperturaCajaService {
    private final AperturaCajaRepository aperturas;
    private final CierreCajaRepository cierres;
    private final UsuarioService usuarios;
    private final Clock reloj;
    @Autowired
    public ReaperturaCajaService(AperturaCajaRepository aperturas, CierreCajaRepository cierres, UsuarioService usuarios) {
        this(aperturas, cierres, usuarios, Clock.system(ZoneId.of("America/Lima")));
    }
    public ReaperturaCajaService(AperturaCajaRepository aperturas, CierreCajaRepository cierres, UsuarioService usuarios, Clock reloj) {
        this.aperturas = aperturas; this.cierres = cierres; this.usuarios = usuarios;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }
    @Transactional
    public void reabrir(ReaperturaCajaForm form, String nombreUsuario) {
        var usuario = usuarios.obtenerUsuarioActivo(nombreUsuario);
        if (usuario.getRol() != RolUsuario.ADMINISTRADOR) throw new IllegalArgumentException("Solo el administrador puede reabrir la caja.");
        var ahora = LocalDateTime.now(reloj);
        if (!ahora.toLocalDate().equals(form.getFecha())) throw new IllegalArgumentException("Solo se puede reabrir la caja de hoy. Actualice la pantalla.");
        if (form.getMotivo() == null || form.getMotivo().isBlank() || form.getMotivo().length() > 200) {
            throw new IllegalArgumentException("Indique un motivo de hasta 200 caracteres.");
        }
        var apertura = aperturas.bloquearPorFecha(ahora.toLocalDate())
                .filter(a -> a.getEstado() == EstadoCaja.CERRADA)
                .orElseThrow(() -> new IllegalArgumentException("La caja de hoy no esta cerrada."));
        var cierre = cierres.findByAperturaCajaIdAndVigenteTrue(apertura.getId())
                .filter(c -> Objects.equals(c.getId(), form.getCierreId()))
                .orElseThrow(() -> new IllegalArgumentException("El cierre cambio. Actualice la pantalla antes de reabrir."));
        cierre.registrarReapertura(usuario, form.getMotivo().strip(), ahora);
        cierres.flush();
        apertura.setEstado(EstadoCaja.ABIERTA);
    }
}
