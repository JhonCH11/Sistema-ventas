package com.minimarket.caja;

import com.minimarket.caja.domain.*;
import com.minimarket.caja.form.*;
import com.minimarket.caja.repository.MovimientoDineroCajaRepository;
import com.minimarket.caja.service.*;
import com.minimarket.usuarios.domain.*;
import com.minimarket.usuarios.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovimientoCajaServiceTest {
    private final CajaService caja = mock(CajaService.class);
    private final MovimientoDineroCajaRepository repo = mock(MovimientoDineroCajaRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final AperturaCaja apertura = mock(AperturaCaja.class);
    private final Usuario admin = mock(Usuario.class);
    private final MovimientoCajaService service = new MovimientoCajaService(caja, repo, usuarios);
    @BeforeEach void preparar() {
        when(caja.bloquearAperturaDeHoy()).thenReturn(apertura);
        when(apertura.getId()).thenReturn(1L);
        when(caja.calcularEfectivoEsperado(apertura)).thenReturn(new BigDecimal("100.00"));
        when(usuarios.obtenerUsuarioActivo("admin")).thenReturn(admin);
        when(admin.getRol()).thenReturn(RolUsuario.ADMINISTRADOR);
    }
    private MovimientoCajaForm form(TipoMovimientoCaja tipo, String importe) {
        var f = new MovimientoCajaForm(); f.setAperturaId(1L); f.setToken(UUID.randomUUID().toString());
        f.setTipo(tipo); f.setImporte(new BigDecimal(importe)); f.setMotivo("Motivo de prueba"); return f;
    }
    @Test void ingresoAumentaEfectivo() {
        service.registrar(form(TipoMovimientoCaja.INGRESO,"50.00"),"admin");
        var captor = ArgumentCaptor.forClass(MovimientoDineroCaja.class); verify(repo).saveAndFlush(captor.capture());
        assertEquals(new BigDecimal("50.00"),captor.getValue().getImpacto());
    }
    @Test void retiroYGastoReducenEfectivo() {
        service.registrar(form(TipoMovimientoCaja.RETIRO,"20.00"),"admin");
        service.registrar(form(TipoMovimientoCaja.GASTO,"5.00"),"admin");
        var captor = ArgumentCaptor.forClass(MovimientoDineroCaja.class); verify(repo,times(2)).saveAndFlush(captor.capture());
        assertEquals(new BigDecimal("-20.00"),captor.getAllValues().get(0).getImpacto());
        assertEquals(new BigDecimal("-5.00"),captor.getAllValues().get(1).getImpacto());
    }
    @Test void rechazaVendedor() {
        when(admin.getRol()).thenReturn(RolUsuario.VENDEDOR);
        assertThrows(IllegalArgumentException.class,()->service.registrar(form(TipoMovimientoCaja.INGRESO,"1.00"),"admin"));
        verify(repo,never()).saveAndFlush(any());
    }
    @Test void rechazaSalidaMayorAlSaldo() {
        assertThrows(IllegalArgumentException.class,()->service.registrar(form(TipoMovimientoCaja.RETIRO,"100.01"),"admin"));
        verify(repo,never()).saveAndFlush(any());
    }
    @Test void rechazaFormularioDeOtraCaja() {
        var f=form(TipoMovimientoCaja.INGRESO,"10.00");f.setAperturaId(2L);
        assertThrows(IllegalArgumentException.class,()->service.registrar(f,"admin"));
    }
    @Test void reintentoNoDuplicaMovimiento() {
        var f=form(TipoMovimientoCaja.INGRESO,"10.00");
        var anterior=new MovimientoDineroCaja(apertura,admin,f.getToken(),f.getTipo(),f.getImporte(),f.getImporte(),f.getMotivo(),LocalDateTime.now(),null);
        when(repo.findByToken(f.getToken())).thenReturn(Optional.of(anterior));
        service.registrar(f,"admin");verify(repo,never()).saveAndFlush(any());
    }
    @Test void validaImportesYMotivo() {
        for(String importe:new String[]{"0","-1","1.001"})assertThrows(IllegalArgumentException.class,()->service.registrar(form(TipoMovimientoCaja.INGRESO,importe),"admin"));
        var f=form(TipoMovimientoCaja.INGRESO,"1.00");f.setMotivo(" ");
        assertThrows(IllegalArgumentException.class,()->service.registrar(f,"admin"));
    }
    @Test void cajaCerradaNoRegistraMovimiento() {
        when(caja.bloquearAperturaDeHoy()).thenThrow(new IllegalArgumentException("Caja cerrada"));
        assertThrows(IllegalArgumentException.class,()->service.registrar(form(TipoMovimientoCaja.GASTO,"1.00"),"admin"));
        verifyNoInteractions(repo);
    }
    @Test void correccionConservaOriginalYRegistraInverso() {
        var original=new MovimientoDineroCaja(apertura,admin,"original",TipoMovimientoCaja.RETIRO,new BigDecimal("20.00"),new BigDecimal("-20.00"),"Retiro",LocalDateTime.now(),null);
        when(repo.findById(7L)).thenReturn(Optional.of(original));
        var f=new CorreccionMovimientoCajaForm();f.setAperturaId(1L);f.setToken(UUID.randomUUID().toString());f.setMotivo("Error al registrar");
        service.corregir(7L,f,"admin");
        var captor=ArgumentCaptor.forClass(MovimientoDineroCaja.class);verify(repo).saveAndFlush(captor.capture());
        assertEquals(new BigDecimal("20.00"),captor.getValue().getImpacto());assertSame(original,captor.getValue().getReversionDe());
        verify(repo,never()).delete(any());
    }
    @Test void noCorrigeDosVeces() {
        var original=mock(MovimientoDineroCaja.class);when(original.getAperturaCaja()).thenReturn(apertura);
        when(repo.findById(7L)).thenReturn(Optional.of(original));when(repo.findByReversionDeId(7L)).thenReturn(Optional.of(mock(MovimientoDineroCaja.class)));
        var f=new CorreccionMovimientoCajaForm();f.setAperturaId(1L);f.setToken(UUID.randomUUID().toString());f.setMotivo("Error");
        assertThrows(IllegalArgumentException.class,()->service.corregir(7L,f,"admin"));verify(repo,never()).saveAndFlush(any());
    }
}
