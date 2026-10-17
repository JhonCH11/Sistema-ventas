package com.minimarket.ventas;

import com.minimarket.productos.domain.Producto;
import com.minimarket.productos.domain.TipoVenta;
import com.minimarket.ventas.form.CarritoVenta;
import com.minimarket.ventas.form.LineaCarritoVenta;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarritoVentaTest {
    @Test
    void redondeaSubtotalPorPesoComercialmente() {
        var linea = new LineaCarritoVenta(1L, "Arroz", TipoVenta.PESO,
                new BigDecimal("3.35"), new BigDecimal("0.50"));
        assertEquals(new BigDecimal("1.68"), linea.getSubtotal());
    }

    @Test
    void editarCantidadConservaPrecioYCancelarRenuevaToken() {
        Producto producto = mock(Producto.class);
        when(producto.getId()).thenReturn(1L);
        when(producto.getNombre()).thenReturn("Arroz");
        when(producto.getTipoVenta()).thenReturn(TipoVenta.PESO);
        when(producto.getPrecioActual()).thenReturn(new BigDecimal("4.00"));
        CarritoVenta carrito = new CarritoVenta();
        carrito.agregar(producto, new BigDecimal("0.50"));
        carrito.actualizar(1L, new BigDecimal("1.25"));
        assertEquals(new BigDecimal("5.00"), carrito.getTotal());
        String token = carrito.getTokenConfirmacion();
        carrito.limpiar();
        assertTrue(carrito.estaVacio());
        assertNotEquals(token, carrito.getTokenConfirmacion());
    }
}
