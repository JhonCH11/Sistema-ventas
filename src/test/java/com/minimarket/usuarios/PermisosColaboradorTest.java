package com.minimarket.usuarios;

import com.minimarket.seguridad.SecurityConfig;
import com.minimarket.usuarios.domain.*;
import com.minimarket.usuarios.repository.UsuarioRepository;
import jakarta.servlet.Filter;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PermisosColaboradorTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private UsuarioRepository repo;
    @Configuration @EnableWebSecurity @EnableWebMvc @Import(SecurityConfig.class)
    static class Config {
        @Bean UsuarioRepository usuarios() { return mock(UsuarioRepository.class); }
        @Bean Rutas rutas() { return new Rutas(); }
    }
    @RestController
    static class Rutas {
        @GetMapping({"/colaboradores", "/precios", "/reportes", "/configuracion/negocio", "/stock/ajustes", "/caja/horario/extender", "/ventas", "/stock", "/caja"})
        String pagina() { return "ok"; }
    }
    @BeforeEach void preparar() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class); context.refresh();
        repo = context.getBean(UsuarioRepository.class);
        when(repo.findByNombreUsuarioAndActivoTrue("prueba")).thenReturn(Optional.of(new Usuario("Prueba", "prueba", "hash", RolUsuario.VENDEDOR)));
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }
    @AfterEach void cerrar() { context.close(); }
    private MockHttpSession sesion(String rol) {
        var sesion = new MockHttpSession();
        var auth = new UsernamePasswordAuthenticationToken("prueba", "", AuthorityUtils.createAuthorityList("ROLE_" + rol));
        sesion.setAttribute("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(auth));
        return sesion;
    }
    @Test void vendedorNoAccedeAAdministracionAunqueEscribaLaUrl() throws Exception {
        for (String ruta : new String[]{"/colaboradores", "/precios", "/reportes", "/configuracion/negocio", "/stock/ajustes", "/caja/horario/extender"}) {
            mvc.perform(get(ruta).session(sesion("VENDEDOR"))).andExpect(status().isForbidden());
        }
    }
    @Test void vendedorPuedeOperarVentasStockYCaja() throws Exception {
        for (String ruta : new String[]{"/ventas", "/stock", "/caja"}) {
            mvc.perform(get(ruta).session(sesion("VENDEDOR"))).andExpect(status().isOk());
        }
    }
    @Test void vendedorNoPuedeReabrirCaja() throws Exception {
        mvc.perform(get("/caja/reapertura").session(sesion("VENDEDOR"))).andExpect(status().isForbidden());
    }
    @Test void vendedorNoPuedeGestionarDineroDeCaja() throws Exception {
        for(String ruta:new String[]{"/caja/movimientos","/caja/movimientos/1/corregir"}) {
            mvc.perform(get(ruta).session(sesion("VENDEDOR"))).andExpect(status().isForbidden());
        }
    }
    @Test void vendedorNoPuedeConsultarHistorialDeCaja() throws Exception {
        for(String ruta:new String[]{"/caja/historial","/caja/historial/1"}) {
            mvc.perform(get(ruta).session(sesion("VENDEDOR"))).andExpect(status().isForbidden());
        }
    }
    @Test void vendedorNoPuedeImportarNiVerResumenAdministrativo() throws Exception {
        for(String ruta:new String[]{"/importaciones/excel","/importaciones/excel/plantilla?tipo=STOCK","/reportes/resumen-diario"}) {
            mvc.perform(get(ruta).session(sesion("VENDEDOR"))).andExpect(status().isForbidden());
        }
    }
    @Test void vendedorNoPuedeGestionarComprasNiProveedores() throws Exception {
        for(String ruta:new String[]{"/compras","/compras/nueva","/compras/1","/compras/proveedores","/compras/proveedores/nuevo"})
            mvc.perform(get(ruta).session(sesion("VENDEDOR"))).andExpect(status().isForbidden());
    }
    @Test void vendedorNoPuedeGestionarLiquidacionesNiMermas() throws Exception {
        for(String ruta:new String[]{"/stock/liquidaciones","/stock/liquidaciones/nueva","/stock/mermas","/stock/mermas/nueva"}) {
            mvc.perform(get(ruta).session(sesion("VENDEDOR"))).andExpect(status().isForbidden());
        }
    }
    @Test void administradorPuedeGestionarColaboradores() throws Exception {
        mvc.perform(get("/colaboradores").session(sesion("ADMINISTRADOR"))).andExpect(status().isOk());
    }
    @Test void cuentaDesactivadaPierdeSesion() throws Exception {
        when(repo.findByNombreUsuarioAndActivoTrue("prueba")).thenReturn(Optional.empty());
        mvc.perform(get("/ventas").session(sesion("VENDEDOR"))).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ingresar?inactivo"));
    }
    @Test void noPermiteCambiosSinCsrf() throws Exception {
        mvc.perform(post("/colaboradores/nuevo").session(sesion("ADMINISTRADOR"))).andExpect(status().isForbidden());
    }
}
