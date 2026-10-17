package com.minimarket.stock;

import com.minimarket.productos.domain.*;
import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.stock.domain.MovimientoStock;
import com.minimarket.stock.form.*;
import com.minimarket.stock.repository.*;
import com.minimarket.stock.service.LiquidacionMermaService;
import com.minimarket.precios.repository.HistorialPrecioRepository;
import com.minimarket.usuarios.domain.*;
import com.minimarket.usuarios.service.UsuarioService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LiquidacionMermaServiceTest {
    private final ProductoRepository productos=mock(ProductoRepository.class);
    private final LiquidacionMermaRepository registros=mock(LiquidacionMermaRepository.class);
    private final MovimientoStockRepository movimientos=mock(MovimientoStockRepository.class);
    private final HistorialPrecioRepository precios=mock(HistorialPrecioRepository.class);
    private final UsuarioService usuarios=mock(UsuarioService.class);
    private final LiquidacionMermaService service=new LiquidacionMermaService(productos,registros,movimientos,precios,usuarios);
    private final Producto producto=spy(new Producto(null,"Platano",TipoVenta.PESO,new BigDecimal("5.00"),new BigDecimal("10.00"),BigDecimal.ZERO));
    private void preparar(){
        when(producto.getId()).thenReturn(10L);
        when(productos.findByIdForUpdate(10L)).thenReturn(Optional.of(producto));
        var admin=mock(Usuario.class);when(admin.getId()).thenReturn(1L);when(admin.getRol()).thenReturn(RolUsuario.ADMINISTRADOR);
        when(usuarios.obtenerUsuarioActivo("admin")).thenReturn(admin);
        when(registros.crearLiquidacion(any(),eq(1L),any())).thenReturn(2L);
        when(productos.saveAndFlush(any(Producto.class))).thenAnswer(i->i.getArgument(0));
    }
    private LiquidacionForm saldo(){
        var f=new LiquidacionForm();f.setProductoId(10L);f.setToken(UUID.randomUUID().toString());f.setCantidad(new BigDecimal("2.00"));
        f.setPrecio(new BigDecimal("3.00"));f.setCostoUnitario(new BigDecimal("3.33"));f.setMotivo("Maduro");return f;
    }
    private MermaForm merma(){
        var f=new MermaForm();f.setProductoId(10L);f.setToken(UUID.randomUUID().toString());f.setCantidad(new BigDecimal("1.25"));
        f.setCostoUnitario(new BigDecimal("3.33"));f.setMotivo(MermaForm.Motivo.DETERIORO);return f;
    }
    @Test void trasladoConservaStockTotalYPrecioNormal(){
        preparar();service.liquidar(saldo(),"admin");
        var captor=org.mockito.ArgumentCaptor.forClass(Producto.class);verify(productos).saveAndFlush(captor.capture());
        var saldo=captor.getValue();assertEquals(new BigDecimal("8.00"),producto.getStockActual());
        assertEquals(new BigDecimal("10.00"),producto.getStockActual().add(saldo.getStockActual()));
        assertEquals(new BigDecimal("5.00"),producto.getPrecioActual());assertEquals(new BigDecimal("3.00"),saldo.getPrecioActual());
        assertTrue(saldo.isLiquidacion());assertEquals(TipoVenta.PESO,saldo.getTipoVenta());
        var ms=org.mockito.ArgumentCaptor.forClass(MovimientoStock.class);verify(movimientos,times(2)).save(ms.capture());
        assertEquals(0,ms.getAllValues().stream().map(MovimientoStock::getCantidad).reduce(BigDecimal.ZERO,BigDecimal::add).signum());
    }
    @Test void precioNoPuedeSerIgualAlNormal(){
        preparar();var f=saldo();f.setPrecio(new BigDecimal("5.00"));
        assertThrows(IllegalArgumentException.class,()->service.liquidar(f,"admin"));verifyNoInteractions(movimientos);
    }
    @Test void noPermiteRetirarMasStock(){
        preparar();var f=merma();f.setCantidad(new BigDecimal("11.00"));
        assertThrows(IllegalArgumentException.class,()->service.registrarMerma(f,"admin"));assertEquals(new BigDecimal("10.00"),producto.getStockActual());
    }
    @Test void calculaMermaAlCostoConRedondeoComercial(){
        preparar();var f=merma();service.registrarMerma(f,"admin");
        assertEquals(new BigDecimal("8.75"),producto.getStockActual());
        verify(registros).crearMerma(eq(f),eq(producto),eq(1L),eq(new BigDecimal("3.33")),eq(new BigDecimal("4.16")),any());
    }
    @Test void mermaDeLiquidacionIgnoraCostoManipulado(){
        preparar();producto.marcarLiquidacion(new BigDecimal("5.00"));when(registros.costoLiquidacion(10L)).thenReturn(new BigDecimal("3.33"));
        var f=merma();f.setCostoUnitario(new BigDecimal("999.00"));service.registrarMerma(f,"admin");
        verify(registros).crearMerma(eq(f),eq(producto),eq(1L),eq(new BigDecimal("3.33")),eq(new BigDecimal("4.16")),any());
    }
    @Test void reintentoDeMermaNoDescuentaDeNuevo(){
        preparar();var f=merma();when(registros.existeMerma(f.getToken())).thenReturn(true);
        service.registrarMerma(f,"admin");assertEquals(new BigDecimal("10.00"),producto.getStockActual());verifyNoInteractions(movimientos);
    }
    @Test void reintentoDeLiquidacionNoDuplicaPartida(){
        preparar();var f=saldo();when(registros.existeLiquidacion(f.getToken())).thenReturn(true);
        service.liquidar(f,"admin");verify(productos,never()).saveAndFlush(any());
    }
    @Test void validaPrecisionYUnidades(){
        assertThrows(IllegalArgumentException.class,()->LiquidacionMermaService.validarCantidad(producto,new BigDecimal("0.123")));
        producto.setTipoVenta(TipoVenta.UNIDAD);
        assertThrows(IllegalArgumentException.class,()->LiquidacionMermaService.validarCantidad(producto,new BigDecimal("0.50")));
    }
    @Test void exigeCostoReal(){
        preparar();var f=merma();f.setCostoUnitario(null);
        assertThrows(IllegalArgumentException.class,()->service.registrarMerma(f,"admin"));verifyNoInteractions(movimientos);
    }
    @Test void vendedorNoPuedeLiquidar(){
        preparar();when(usuarios.obtenerUsuarioActivo("admin").getRol()).thenReturn(RolUsuario.VENDEDOR);
        assertThrows(IllegalArgumentException.class,()->service.liquidar(saldo(),"admin"));verifyNoInteractions(registros);
    }
}
