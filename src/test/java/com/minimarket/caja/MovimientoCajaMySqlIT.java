package com.minimarket.caja;

import com.minimarket.caja.domain.*;
import com.minimarket.caja.form.*;
import com.minimarket.caja.repository.MovimientoDineroCajaRepository;
import com.minimarket.caja.service.*;
import com.minimarket.usuarios.domain.*;
import com.minimarket.usuarios.repository.UsuarioRepository;
import com.minimarket.usuarios.service.UsuarioAutenticacionService.UsuarioSesion;
import jakarta.servlet.Filter;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfSystemProperty(named="minimarket.prueba.puerto",matches="[0-9]+")
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:mysql://127.0.0.1:${minimarket.prueba.puerto}/minimarket_liquidacion_prueba?serverTimezone=America/Lima",
    "spring.datasource.username=root", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true", "spring.flyway.clean-disabled=true"})
class MovimientoCajaMySqlIT {
    @Autowired MovimientoCajaService movimientos;
    @Autowired MovimientoDineroCajaRepository repo;
    @Autowired CajaService caja;
    @Autowired ReaperturaCajaService reapertura;
    @Autowired UsuarioRepository usuarios;
    @Autowired WebApplicationContext web;
    @Autowired HistorialCajaService historial;
    @Autowired com.minimarket.caja.repository.HorarioCajaRepository horarios;
    private Usuario admin;
    private AperturaCaja apertura;
    @BeforeEach void preparar() {
        admin=usuarios.saveAndFlush(new Usuario("Admin prueba","admin-"+UUID.randomUUID(),"hash-prueba",RolUsuario.ADMINISTRADOR));
        var principal=new UsuarioSesion(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"",principal.getAuthorities()));
        if(caja.buscarAperturaDeHoy().isEmpty()) {
            var form=new AperturaCajaForm();form.setEfectivoInicial(new BigDecimal("100.00"));caja.abrirCaja(form,admin.getNombreUsuario());
        }
        apertura=caja.buscarAperturaDeHoy().orElseThrow();
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    private MovimientoCajaForm form(TipoMovimientoCaja tipo,String importe) {
        var f=new MovimientoCajaForm();f.setAperturaId(apertura.getId());f.setToken(UUID.randomUUID().toString());
        f.setTipo(tipo);f.setImporte(new BigDecimal(importe));f.setMotivo("Prueba de dinero");return f;
    }
    @Test void registraCorrigeYConservaCierreTrasReabrir() throws Exception {
        var base=caja.calcularEfectivoEsperado(apertura);
        var ingreso=form(TipoMovimientoCaja.INGRESO,"50.00");movimientos.registrar(ingreso,admin.getNombreUsuario());movimientos.registrar(ingreso,admin.getNombreUsuario());
        var retiro=form(TipoMovimientoCaja.RETIRO,"20.00");movimientos.registrar(retiro,admin.getNombreUsuario());
        movimientos.registrar(form(TipoMovimientoCaja.GASTO,"5.00"),admin.getNombreUsuario());
        assertEquals(base.add(new BigDecimal("25.00")),caja.calcularEfectivoEsperado(apertura));
        var original=repo.findByAperturaCajaIdOrderByIdDesc(apertura.getId()).stream().filter(m->m.getTipo()==TipoMovimientoCaja.RETIRO&&m.getUsuario().getId().equals(admin.getId())).findFirst().orElseThrow();
        var correccion=new CorreccionMovimientoCajaForm();correccion.setAperturaId(apertura.getId());correccion.setToken(UUID.randomUUID().toString());correccion.setMotivo("Retiro errado");
        movimientos.corregir(original.getId(),correccion,admin.getNombreUsuario());movimientos.corregir(original.getId(),correccion,admin.getNombreUsuario());
        var esperado=base.add(new BigDecimal("45.00"));assertEquals(esperado,caja.calcularEfectivoEsperado(apertura));
        var cierreForm=new CierreCajaForm();cierreForm.setEfectivoContado(esperado);caja.cerrarCaja(cierreForm,admin.getNombreUsuario());
        var cierre=caja.buscarCierre(apertura).orElseThrow();assertEquals(esperado,cierre.getEfectivoEsperado());
        var hoy=LocalDate.now(ZoneId.of("America/Lima"));
        assertEquals(esperado,historial.listar(hoy,hoy).get(0).esperado());
        assertEquals(esperado,historial.detalle(apertura.getId()).cierres().stream().filter(c->c.id()==cierre.getId()).findFirst().orElseThrow().esperado());
        comprobarPantallasHistorial();
        assertThrows(IllegalArgumentException.class,()->movimientos.registrar(form(TipoMovimientoCaja.INGRESO,"1.00"),admin.getNombreUsuario()));
        var reabrir=new ReaperturaCajaForm();reabrir.setFecha(LocalDate.now(ZoneId.of("America/Lima")));reabrir.setCierreId(cierre.getId());reabrir.setMotivo("Continuar pruebas");
        reapertura.reabrir(reabrir,admin.getNombreUsuario());
        assertEquals(esperado,caja.calcularEfectivoEsperado(apertura));
        assertNull(historial.listar(hoy,hoy).get(0).esperado());
        var cierrePrevio=historial.detalle(apertura.getId()).cierres().stream().filter(c->c.id()==cierre.getId()).findFirst().orElseThrow();
        assertFalse(cierrePrevio.vigente());assertNotNull(cierrePrevio.reabiertaEn());assertEquals("Continuar pruebas",cierrePrevio.motivoReapertura());
        horarios.registrar(apertura.getId(),admin.getId(),LocalTime.of(21,0),LocalTime.of(22,0),"Primera extension de prueba");
        horarios.registrar(apertura.getId(),admin.getId(),LocalTime.of(22,0),LocalTime.of(23,0),"Segunda extension de prueba");
        assertEquals(1,historial.listar(hoy,hoy).size());
        assertEquals(2,historial.detalle(apertura.getId()).extensiones().size());
        assertTrue(historial.listar(hoy.minusDays(2),hoy.minusDays(1)).isEmpty());
        movimientos.registrar(form(TipoMovimientoCaja.GASTO,"1.00"),admin.getNombreUsuario());
        assertEquals(esperado.subtract(BigDecimal.ONE),caja.calcularEfectivoEsperado(apertura));
        assertEquals(esperado,historial.detalle(apertura.getId()).cierres().stream().filter(c->c.id()==cierre.getId()).findFirst().orElseThrow().esperado());
        comprobarPantallasHistorial();
    }
    @Test void retirosSimultaneosNoSobregiranLaCaja() throws Exception {
        var base=caja.calcularEfectivoEsperado(apertura);
        var importe=base.multiply(new BigDecimal("0.75")).setScale(2,RoundingMode.HALF_UP);
        var f1=form(TipoMovimientoCaja.RETIRO,importe.toPlainString());var f2=form(TipoMovimientoCaja.RETIRO,importe.toPlainString());
        var auth=SecurityContextHolder.getContext().getAuthentication();
        var inicio=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> tareas=new ArrayList<>();
            for(var form:List.of(f1,f2))tareas.add(pool.submit(()->{
                SecurityContextHolder.getContext().setAuthentication(auth);
                try { inicio.await();movimientos.registrar(form,admin.getNombreUsuario());return true; }
                catch(IllegalArgumentException ex){return false;}finally{SecurityContextHolder.clearContext();}
            }));
            inicio.countDown();int exitos=0;for(var tarea:tareas)if(tarea.get(15,TimeUnit.SECONDS))exitos++;
            assertEquals(1,exitos);assertEquals(base.subtract(importe),caja.calcularEfectivoEsperado(apertura));
        } finally { pool.shutdownNow();pool.awaitTermination(10,TimeUnit.SECONDS); }
    }
    @Test void renderizaListadoFormularioYCorreccion() throws Exception {
        movimientos.registrar(form(TipoMovimientoCaja.INGRESO,"1.00"),admin.getNombreUsuario());
        var session=new MockHttpSession();session.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(SecurityContextHolder.getContext().getAuthentication()));
        var mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(web.getBean("springSecurityFilterChain",Filter.class)).build();
        mvc.perform(get("/caja/movimientos").session(session)).andExpect(status().isOk());
        mvc.perform(get("/caja").session(session)).andExpect(status().isOk());
        comprobarPantallasHistorial();
    }
    private void comprobarPantallasHistorial() throws Exception {
        var principal=new UsuarioSesion(admin);
        var auth=new UsernamePasswordAuthenticationToken(principal,"",principal.getAuthorities());
        var session=new MockHttpSession();session.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(auth));
        var mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(web.getBean("springSecurityFilterChain",Filter.class)).build();
        try {
            var lista=mvc.perform(get("/caja/historial").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var detalle=mvc.perform(get("/caja/historial/"+apertura.getId()).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            mvc.perform(get("/caja/historial").param("desde","2000-01-01").param("hasta","2000-01-02").session(session)).andExpect(status().isOk());
            var salida=java.nio.file.Path.of("target","vistas-prueba");java.nio.file.Files.createDirectories(salida);
            java.nio.file.Files.writeString(salida.resolve("historial-lista.html"),lista);
            java.nio.file.Files.writeString(salida.resolve("historial-detalle.html"),detalle);
        } finally { SecurityContextHolder.getContext().setAuthentication(auth); }
    }
}
