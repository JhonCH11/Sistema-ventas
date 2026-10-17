package com.minimarket.stock;

import com.minimarket.productos.domain.TipoVenta;
import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.stock.form.AlertaStock;
import com.minimarket.stock.service.AlertaStockService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlertaStockServiceTest {
    @Test void cuentaPorProductoSinSumarKilosConUnidades() {
        var repo=mock(ProductoRepository.class);
        var agotado=new AlertaStock(1L,"Arroz",TipoVenta.PESO,BigDecimal.ZERO,new BigDecimal("5.00"));
        var bajo=new AlertaStock(2L,"Gaseosa",TipoVenta.UNIDAD,new BigDecimal("2.00"),new BigDecimal("2.00"));
        when(repo.buscarAlertasStock()).thenReturn(List.of(agotado,bajo));
        var resumen=new AlertaStockService(repo).consultar();
        assertEquals(1,resumen.agotados());assertEquals(1,resumen.bajos());assertEquals(2,resumen.getTotal());
        assertTrue(agotado.isAgotado());assertFalse(bajo.isAgotado());assertEquals("kg",agotado.getUnidad());assertEquals("unidades",bajo.getUnidad());
    }
    @Test void resumenLimitaDestacadosSinPerderCantidadTotal() {
        var repo=mock(ProductoRepository.class);var filas=new ArrayList<AlertaStock>();
        for(long id=1;id<=9;id++)filas.add(new AlertaStock(id,"Producto "+id,TipoVenta.UNIDAD,BigDecimal.ZERO,BigDecimal.ONE));
        when(repo.buscarAlertasStock()).thenReturn(filas);
        var resumen=new AlertaStockService(repo).consultar();
        assertEquals(9,resumen.getTotal());assertEquals(6,resumen.getDestacados().size());assertEquals(9,resumen.agotados());
    }
    @Test void manejaInventarioSinAlertas() {
        var repo=mock(ProductoRepository.class);when(repo.buscarAlertasStock()).thenReturn(List.of());
        var resumen=new AlertaStockService(repo).consultar();assertEquals(0,resumen.getTotal());assertTrue(resumen.getDestacados().isEmpty());
    }
    @Test void reponerPreseleccionaProductoSinModificarStock() {
        var stock=mock(com.minimarket.stock.service.StockService.class);var model=new org.springframework.ui.ExtendedModelMap();
        assertEquals("stock/lista",new com.minimarket.stock.controller.StockController(stock).mostrarPantalla(model,12L));
        assertEquals(12L,((com.minimarket.stock.form.IngresoStockForm)model.get("formulario")).getProductoId());
        verify(stock,never()).registrarIngreso(any(),any());
    }
}
