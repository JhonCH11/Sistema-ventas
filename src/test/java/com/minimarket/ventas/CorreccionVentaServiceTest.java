package com.minimarket.ventas;

import com.minimarket.ventas.service.CorreccionVentaService;
import com.minimarket.ventas.repository.*;
import com.minimarket.caja.service.CajaService;
import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.stock.repository.MovimientoStockRepository;
import com.minimarket.usuarios.repository.UsuarioRepository;
import com.minimarket.usuarios.domain.Usuario;
import com.minimarket.usuarios.domain.RolUsuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorreccionVentaServiceTest {
    @Test
    void exigeDecisionYRechazaReponerProductoDanado() {
        assertThrows(IllegalArgumentException.class,
                () -> CorreccionVentaService.validarDestinoStock("PRODUCTO_EQUIVOCADO", null));
        assertThrows(IllegalArgumentException.class,
                () -> CorreccionVentaService.validarDestinoStock("PRODUCTO_MAL_ESTADO", true));
        assertDoesNotThrow(() -> CorreccionVentaService.validarDestinoStock("PRODUCTO_MAL_ESTADO", false));
        assertDoesNotThrow(() -> CorreccionVentaService.validarDestinoStock("PRODUCTO_EQUIVOCADO", true));
        assertDoesNotThrow(() -> CorreccionVentaService.validarDestinoStock("PRODUCTO_EQUIVOCADO", false));
    }
    @Test
    void claveIncorrectaNoActualizaCajaNiStock() {
        var usuarios=mock(UsuarioRepository.class);
        var encoder=mock(PasswordEncoder.class);
        var caja=mock(CajaService.class);
        var productos=mock(ProductoRepository.class);
        var movimientos=mock(MovimientoStockRepository.class);
        when(usuarios.findByNombreUsuarioAndActivoTrue("admin")).thenReturn(Optional.of(
                new Usuario("Administrador","admin","hash",RolUsuario.ADMINISTRADOR)));
        when(encoder.matches("incorrecta","hash")).thenReturn(false);
        var servicio=new CorreccionVentaService(mock(VentaRepository.class),
                mock(VentaDetalleRepository.class),mock(CorreccionVentaRepository.class),
                usuarios,encoder,caja,productos,movimientos);
        assertThrows(IllegalArgumentException.class,
                ()->servicio.anular("T-1","admin","incorrecta","ERROR_REGISTRO",""));
        verifyNoInteractions(caja,productos,movimientos);
    }
}
