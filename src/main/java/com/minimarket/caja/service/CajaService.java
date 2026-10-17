package com.minimarket.caja.service;

import com.minimarket.caja.domain.AperturaCaja;
import com.minimarket.caja.domain.CierreCaja;
import com.minimarket.caja.domain.EstadoCaja;
import com.minimarket.caja.form.AperturaCajaForm;
import com.minimarket.caja.form.CierreCajaForm;
import com.minimarket.caja.repository.AperturaCajaRepository;
import com.minimarket.caja.repository.CierreCajaRepository;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.service.UsuarioService;
import com.minimarket.ventas.domain.MedioPago;
import com.minimarket.ventas.domain.EstadoVenta;
import com.minimarket.ventas.repository.PagoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CajaService {

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Lima");

    private final AperturaCajaRepository aperturaCajaRepository;
    private final CierreCajaRepository cierreCajaRepository;
    private final UsuarioService usuarioService;
    private final PagoRepository pagoRepository;
    private final com.minimarket.ventas.repository.CorreccionVentaRepository correcciones;
    private final com.minimarket.caja.repository.MovimientoDineroCajaRepository movimientos;

    public CajaService(
            AperturaCajaRepository aperturaCajaRepository,
            CierreCajaRepository cierreCajaRepository,
            UsuarioService usuarioService,
            PagoRepository pagoRepository,
            com.minimarket.ventas.repository.CorreccionVentaRepository correcciones,
            com.minimarket.caja.repository.MovimientoDineroCajaRepository movimientos) {
        this.aperturaCajaRepository = aperturaCajaRepository;
        this.cierreCajaRepository = cierreCajaRepository;
        this.usuarioService = usuarioService;
        this.pagoRepository = pagoRepository;
        this.correcciones = correcciones;
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    public Optional<AperturaCaja> buscarAperturaDeHoy() {
        return aperturaCajaRepository.findByFechaOperacion(fechaActual());
    }

    @Transactional
    public void abrirCaja(AperturaCajaForm formulario, String nombreUsuario) {
        BigDecimal efectivoInicial = formulario.getEfectivoInicial();
        if (efectivoInicial.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El efectivo inicial permite como máximo dos decimales.");
        }

        LocalDate fecha = fechaActual();
        if (aperturaCajaRepository.findByFechaOperacion(fecha).isPresent()) {
            throw new IllegalArgumentException("La caja de hoy ya fue abierta.");
        }

        Usuario usuario = usuarioService.obtenerUsuarioActivo(nombreUsuario);
        aperturaCajaRepository.save(new AperturaCaja(usuario, fecha, efectivoInicial));
    }

    @Transactional(readOnly = true)
    public AperturaCaja obtenerAperturaAbiertaDeHoy() {
        return aperturaCajaRepository.findByFechaOperacion(fechaActual())
                .filter(apertura -> apertura.getEstado() == EstadoCaja.ABIERTA)
                .orElseThrow(() -> new IllegalArgumentException("No hay una caja abierta para hoy."));
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularEfectivoEsperado(AperturaCaja apertura) {
        BigDecimal ventasEfectivo = pagoRepository.totalCobradoPorAperturaYMedioPago(
                apertura.getId(), MedioPago.EFECTIVO, EstadoVenta.CONFIRMADA);
        return apertura.getEfectivoInicial().add(ventasEfectivo == null ? BigDecimal.ZERO : ventasEfectivo)
                .subtract(correcciones.reembolsosEfectivo(apertura.getId()))
                .add(movimientos.impactoTotal(apertura.getId()));
    }

    @Transactional(readOnly = true)
    public Optional<CierreCaja> buscarCierre(AperturaCaja apertura) {
        return cierreCajaRepository.findByAperturaCajaIdAndVigenteTrue(apertura.getId());
    }

    @Transactional
    public void cerrarCaja(CierreCajaForm formulario, String nombreUsuario) {
        AperturaCaja apertura = bloquearAperturaDeHoy();
        BigDecimal efectivoContado = formulario.getEfectivoContado();
        if (efectivoContado.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El efectivo contado permite como máximo dos decimales.");
        }
        if (cierreCajaRepository.existsByAperturaCajaIdAndVigenteTrue(apertura.getId())) {
            throw new IllegalArgumentException("La caja de hoy ya fue cerrada.");
        }

        Usuario usuario = usuarioService.obtenerUsuarioActivo(nombreUsuario);
        BigDecimal esperado = calcularEfectivoEsperado(apertura);
        String observacion = textoOpcional(formulario.getObservacion());
        cierreCajaRepository.save(new CierreCaja(apertura, usuario, esperado, efectivoContado, observacion));
        apertura.setEstado(EstadoCaja.CERRADA);
    }

    private String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    @Transactional
    public AperturaCaja bloquearAperturaDeHoy() {
        return aperturaCajaRepository.bloquearPorFecha(fechaActual())
                .filter(a -> a.getEstado() == EstadoCaja.ABIERTA)
                .orElseThrow(() -> new IllegalArgumentException("La caja de hoy no esta abierta."));
    }

    private LocalDate fechaActual() {
        return LocalDate.now(ZONA_HORARIA);
    }
}
