package com.minimarket.stock;
import com.minimarket.stock.service.StockService;
import com.minimarket.stock.form.AjusteStockForm.Motivo;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AjusteStockTest {
    @Test void conteoRegistraDiferenciaReal() {
        assertEquals(new BigDecimal("-2.50"), StockService.calcularDiferencia(
                new BigDecimal("10.00"),new BigDecimal("7.50"),Motivo.CONTEO_FISICO));
        assertEquals(new BigDecimal("2.00"), StockService.calcularDiferencia(
                new BigDecimal("10.00"),new BigDecimal("12.00"),Motivo.CONTEO_FISICO));
    }
    @Test void perdidasNoPermitenStockNegativo() {
        assertThrows(IllegalArgumentException.class,()->StockService.calcularDiferencia(
                new BigDecimal("2"),new BigDecimal("3"),Motivo.PERDIDA));
        assertEquals(new BigDecimal("-2"),StockService.calcularDiferencia(
                new BigDecimal("2"),new BigDecimal("2"),Motivo.PRODUCTO_VENCIDO));
    }
    @Test void noRegistraMovimientosSinDiferencia() {
        assertThrows(IllegalArgumentException.class,()->StockService.calcularDiferencia(
                new BigDecimal("2"),new BigDecimal("2"),Motivo.CONTEO_FISICO));
    }
}
