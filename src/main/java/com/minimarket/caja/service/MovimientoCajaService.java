package com.minimarket.caja.service;

import com.minimarket.caja.domain.*;
import com.minimarket.caja.form.*;
import com.minimarket.caja.repository.MovimientoDineroCajaRepository;
import com.minimarket.usuarios.domain.*;
import com.minimarket.usuarios.service.UsuarioService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class MovimientoCajaService {
    private final CajaService caja;
    private final MovimientoDineroCajaRepository movimientos;
    private final UsuarioService usuarios;
    public MovimientoCajaService(CajaService caja, MovimientoDineroCajaRepository movimientos, UsuarioService usuarios) {
        this.caja = caja; this.movimientos = movimientos; this.usuarios = usuarios;
    }
    @Transactional(readOnly = true)
    public List<MovimientoDineroCaja> listar(Long aperturaId) { return movimientos.findByAperturaCajaIdOrderByIdDesc(aperturaId); }

    @Transactional
    public void registrar(MovimientoCajaForm form, String nombreUsuario) {
        validarTokenYMotivo(form.getToken(), form.getMotivo());
        var apertura = caja.bloquearAperturaDeHoy();
        var usuario = administrador(nombreUsuario);
        validarApertura(apertura, form.getAperturaId());
        if (registrado(form.getToken(), apertura)) return;
        var importe = form.getImporte();
        if (form.getTipo() == null || importe == null || importe.signum() <= 0 || importe.stripTrailingZeros().scale() > 2
                || importe.precision() - importe.scale() > 10) throw new IllegalArgumentException("Seleccione el tipo e ingrese un importe positivo con hasta dos decimales.");
        var impacto = form.getTipo() == TipoMovimientoCaja.INGRESO ? importe : importe.negate();
        validarSaldo(apertura, impacto);
        movimientos.saveAndFlush(new MovimientoDineroCaja(apertura, usuario, form.getToken(), form.getTipo(), importe,
                impacto, form.getMotivo().strip(), LocalDateTime.now(ZoneId.of("America/Lima")), null));
    }
    @Transactional
    public void corregir(Long id, CorreccionMovimientoCajaForm form, String nombreUsuario) {
        validarTokenYMotivo(form.getToken(), form.getMotivo());
        var apertura = caja.bloquearAperturaDeHoy();
        var usuario = administrador(nombreUsuario);
        validarApertura(apertura, form.getAperturaId());
        if (registrado(form.getToken(), apertura)) return;
        var original = movimientos.findById(id).filter(m -> Objects.equals(m.getAperturaCaja().getId(), apertura.getId()))
                .orElseThrow(() -> new IllegalArgumentException("El movimiento no pertenece a la caja de hoy."));
        if (original.getReversionDe() != null) throw new IllegalArgumentException("No se puede corregir una correccion. Registre un nuevo movimiento si corresponde.");
        if (movimientos.findByReversionDeId(id).isPresent()) throw new IllegalArgumentException("Este movimiento ya fue corregido.");
        var impacto = original.getImpacto().negate();
        validarSaldo(apertura, impacto);
        movimientos.saveAndFlush(new MovimientoDineroCaja(apertura, usuario, form.getToken(), original.getTipo(),
                original.getImporte(), impacto, form.getMotivo().strip(), LocalDateTime.now(ZoneId.of("America/Lima")), original));
    }
    private Usuario administrador(String nombre) {
        var usuario = usuarios.obtenerUsuarioActivo(nombre);
        if (usuario.getRol() != RolUsuario.ADMINISTRADOR) throw new IllegalArgumentException("Solo el administrador puede registrar movimientos de caja.");
        return usuario;
    }
    private boolean registrado(String token, AperturaCaja apertura) {
        var anterior = movimientos.findByToken(token);
        if (anterior.isPresent() && !Objects.equals(anterior.get().getAperturaCaja().getId(), apertura.getId()))
            throw new IllegalArgumentException("El formulario pertenece a otra caja. Actualice la pantalla.");
        return anterior.isPresent();
    }
    private void validarApertura(AperturaCaja apertura, Long id) {
        if (!Objects.equals(apertura.getId(), id)) throw new IllegalArgumentException("La caja cambio. Actualice la pantalla.");
    }
    private void validarSaldo(AperturaCaja apertura, BigDecimal impacto) {
        var esperado = caja.calcularEfectivoEsperado(apertura);
        if (impacto.signum() < 0 && esperado.add(impacto).signum() < 0)
            throw new IllegalArgumentException("El importe supera el efectivo esperado disponible: S/ " + esperado + ".");
    }
    private void validarTokenYMotivo(String token, String motivo) {
        try { if (!UUID.fromString(token).toString().equals(token)) throw new IllegalArgumentException(); }
        catch (Exception ex) { throw new IllegalArgumentException("Actualice el formulario antes de guardar."); }
        if (motivo == null || motivo.isBlank() || motivo.length() > 200) throw new IllegalArgumentException("Indique un motivo de hasta 200 caracteres.");
    }
}
