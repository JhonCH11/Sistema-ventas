package com.minimarket.usuarios;

import com.minimarket.usuarios.controller.ColaboradorController;
import com.minimarket.usuarios.form.ColaboradorForm;
import com.minimarket.usuarios.service.ColaboradorService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdicionColaboradorControllerTest {
    @Test void conservaFormularioCuandoElUsuarioEstaOcupado() {
        var service = mock(ColaboradorService.class);
        var form = new ColaboradorForm(); form.setNombreUsuario("ocupado");
        doThrow(new IllegalArgumentException("Ese usuario ya existe.")).when(service).editar(2L, form);
        var model = new ExtendedModelMap();
        assertEquals("usuarios/colaborador-formulario", new ColaboradorController(service).guardarEdicion(2L,
                form, mock(BindingResult.class), model, new RedirectAttributesModelMap()));
        assertEquals(2L, model.get("colaboradorId"));
        assertEquals("Ese usuario ya existe.", model.get("error"));
        assertEquals("ocupado", form.getNombreUsuario());
    }

    @Test void validacionFallidaNoGuarda() {
        var service = mock(ColaboradorService.class);
        var resultado = mock(BindingResult.class);
        when(resultado.hasErrors()).thenReturn(true);
        assertEquals("usuarios/colaborador-formulario", new ColaboradorController(service).guardarEdicion(2L,
                new ColaboradorForm(), resultado, new ExtendedModelMap(), new RedirectAttributesModelMap()));
        verifyNoInteractions(service);
    }
}
