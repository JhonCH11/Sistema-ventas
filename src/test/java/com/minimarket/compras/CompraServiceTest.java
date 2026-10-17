package com.minimarket.compras;

import com.minimarket.compras.service.CompraService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompraServiceTest {
    @Test void redondeoComercialPorLinea(){assertEquals(new BigDecimal("1.67"),CompraService.subtotal(new BigDecimal("0.50"),new BigDecimal("3.33")));}
    @Test void costoExactoPorUnidad(){assertEquals(new BigDecimal("5.00"),CompraService.subtotal(new BigDecimal("2"),new BigDecimal("2.50")));}
}
