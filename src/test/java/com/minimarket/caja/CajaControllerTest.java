package com.minimarket.caja;

import com.minimarket.caja.controller.CajaController;
import com.minimarket.caja.form.CierreCajaForm;
import com.minimarket.caja.service.CajaService;
import com.minimarket.respaldos.service.RespaldoService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CajaControllerTest {
    @Test
    void mantieneFormularioAbiertoCuandoHayErroresDeValidacion() {
        var caja = mock(CajaService.class);
        var respaldo = mock(RespaldoService.class);
        var resultado = mock(BindingResult.class);
        when(resultado.hasErrors()).thenReturn(true);
        var model = new ExtendedModelMap();
        String vista = new CajaController(caja, respaldo).cerrarCaja(new CierreCajaForm(),
                resultado, mock(Authentication.class), model, new RedirectAttributesModelMap());
        assertEquals("caja/principal", vista);
        assertEquals(true, model.get("cierreExpandido"));
        verify(caja, never()).cerrarCaja(any(), any());
        verifyNoInteractions(respaldo);
    }

    @Test
    void mantieneFormularioAbiertoSiElServicioRechazaElCierre() {
        var caja = mock(CajaService.class);
        var respaldo = mock(RespaldoService.class);
        var authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        doThrow(new IllegalArgumentException("La caja no esta abierta.")).when(caja).cerrarCaja(any(), eq("admin"));
        var model = new ExtendedModelMap();
        String vista = new CajaController(caja, respaldo).cerrarCaja(new CierreCajaForm(),
                mock(BindingResult.class), authentication, model, new RedirectAttributesModelMap());
        assertEquals("caja/principal", vista);
        assertEquals(true, model.get("cierreExpandido"));
        assertEquals("La caja no esta abierta.", model.get("errorGeneral"));
        verifyNoInteractions(respaldo);
    }
}
