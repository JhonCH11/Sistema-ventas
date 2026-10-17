package com.minimarket.compras;

import com.minimarket.compras.form.*;
import com.minimarket.compras.repository.*;
import com.minimarket.compras.service.*;
import com.minimarket.productos.domain.*;
import com.minimarket.productos.repository.*;
import com.minimarket.stock.form.IngresoStockForm;
import com.minimarket.usuarios.domain.*;
import com.minimarket.usuarios.repository.UsuarioRepository;
import com.minimarket.usuarios.service.UsuarioAutenticacionService.UsuarioSesion;
import jakarta.servlet.Filter;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfSystemProperty(named="minimarket.prueba.puerto",matches="[0-9]+")
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:mysql://127.0.0.1:${minimarket.prueba.puerto}/minimarket_liquidacion_prueba?serverTimezone=America/Lima",
    "spring.datasource.username=root","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true","spring.flyway.clean-disabled=true"})
class CompraMySqlIT {
    @Autowired CompraService compras;
    @Autowired ProveedorService proveedores;
    @Autowired ProveedorRepository proveedorRepo;
    @Autowired CompraRepository repo;
    @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias;
    @Autowired UsuarioRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext web;
    private Usuario admin;
    private Producto fruta,bebida;
    private Long proveedorId;
    @BeforeEach void preparar(){
        String id=UUID.randomUUID().toString();
        admin=usuarios.saveAndFlush(new Usuario("Admin compras","compras-"+id,"hash",RolUsuario.ADMINISTRADOR));autenticar();
        var cat=categorias.saveAndFlush(new Categoria("Compras "+id));
        fruta=productos.saveAndFlush(new Producto(cat,"Mango "+id,TipoVenta.PESO,new BigDecimal("5.00"),new BigDecimal("10.00"),BigDecimal.ZERO));
        bebida=productos.saveAndFlush(new Producto(cat,"Gaseosa "+id,TipoVenta.UNIDAD,new BigDecimal("6.00"),new BigDecimal("10.00"),BigDecimal.ZERO));
        var p=new ProveedorForm();p.setNombre("Proveedor "+id);p.setTelefono("999000111");proveedores.guardar(null,p);
        proveedorId=jdbc.queryForObject("SELECT id FROM proveedores WHERE nombre=?",Long.class,p.getNombre());
    }
    private void autenticar(){var principal=new UsuarioSesion(admin);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"",principal.getAuthorities()));}
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();}
    private IngresoStockForm linea(Producto producto,String cantidad,String costo){var f=new IngresoStockForm();f.setProductoId(producto.getId());f.setCantidad(new BigDecimal(cantidad));f.setCostoUnitario(new BigDecimal(costo));return f;}
    private CompraForm formulario(){var f=new CompraForm();f.setToken(UUID.randomUUID().toString());f.setProveedorId(proveedorId);f.setDocumento("F-"+UUID.randomUUID());f.getLineas().add(linea(fruta,"0.50","3.33"));f.getLineas().add(linea(bebida,"2","2.50"));return f;}
    @Test void variosProductosTotalHistoricoYReintento(){
        var f=formulario();Long id=compras.confirmar(f,admin.getNombreUsuario());assertEquals(id,compras.confirmar(f,admin.getNombreUsuario()));
        assertEquals(new BigDecimal("6.67"),compras.obtener(id).total());
        assertEquals(new BigDecimal("10.50"),productos.findById(fruta.getId()).orElseThrow().getStockActual());
        assertEquals(new BigDecimal("12.00"),productos.findById(bebida.getId()).orElseThrow().getStockActual());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM movimientos_stock WHERE referencia_tipo='INGRESO_MERCADERIA' AND referencia_id=?",Integer.class,id));
        String proveedor=compras.obtener(id).proveedor(),nombre=compras.detalles(id).get(0).producto();
        jdbc.update("UPDATE proveedores SET nombre=? WHERE id=?","Otro nombre "+UUID.randomUUID(),proveedorId);
        jdbc.update("UPDATE productos SET nombre=? WHERE id=?","Otro mango "+UUID.randomUUID(),fruta.getId());
        assertEquals(proveedor,compras.obtener(id).proveedor());assertEquals(nombre,compras.detalles(id).get(0).producto());
    }
    @Test void documentoDuplicadoNoIngresaOtraVez(){
        var f=formulario();compras.confirmar(f,admin.getNombreUsuario());f.setToken(UUID.randomUUID().toString());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->compras.confirmar(f,admin.getNombreUsuario()));
        assertEquals(new BigDecimal("10.50"),productos.findById(fruta.getId()).orElseThrow().getStockActual());
    }
    @Test void falloEnSegundaLineaRevierteTodo(){
        var f=formulario();
        jdbc.execute("CREATE TRIGGER fallo_compra BEFORE INSERT ON movimientos_stock FOR EACH ROW BEGIN IF NEW.producto_id="+bebida.getId()+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo compra de prueba'; END IF; END");
        try{
            assertThrows(RuntimeException.class,()->compras.confirmar(f,admin.getNombreUsuario()));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ingresos_mercaderia WHERE token_compra=?",Integer.class,f.getToken()));
            assertEquals(new BigDecimal("10.00"),productos.findById(fruta.getId()).orElseThrow().getStockActual());
            assertEquals(new BigDecimal("10.00"),productos.findById(bebida.getId()).orElseThrow().getStockActual());
        }finally{jdbc.execute("DROP TRIGGER fallo_compra");}
    }
    @Test void validaUnidadesDuplicadosYProveedorInactivo(){
        var f=formulario();f.getLineas().get(1).setCantidad(new BigDecimal("0.50"));
        assertThrows(IllegalArgumentException.class,()->compras.confirmar(f,admin.getNombreUsuario()));
        f.getLineas().get(1).setCantidad(new BigDecimal("2"));f.getLineas().add(linea(fruta,"1","2"));
        assertThrows(IllegalArgumentException.class,()->compras.confirmar(f,admin.getNombreUsuario()));
        f.getLineas().remove(2);jdbc.update("UPDATE proveedores SET activo=0 WHERE id=?",proveedorId);
        assertThrows(IllegalArgumentException.class,()->compras.confirmar(f,admin.getNombreUsuario()));
        assertEquals(new BigDecimal("10.00"),productos.findById(fruta.getId()).orElseThrow().getStockActual());
    }
    @Test void comprasConcurrentesNoPierdenStock() throws Exception {
        var primera=formulario();var segunda=formulario();segunda.setProveedorId(null);segunda.setDocumento(null);
        try(var ejecutor=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var a=ejecutor.submit(()->{autenticar();try{return compras.confirmar(primera,admin.getNombreUsuario());}finally{SecurityContextHolder.clearContext();}});
            var b=ejecutor.submit(()->{autenticar();try{return compras.confirmar(segunda,admin.getNombreUsuario());}finally{SecurityContextHolder.clearContext();}});
            assertNotEquals(a.get(30,java.util.concurrent.TimeUnit.SECONDS),b.get(30,java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(new BigDecimal("11.00"),productos.findById(fruta.getId()).orElseThrow().getStockActual());
        assertEquals(new BigDecimal("14.00"),productos.findById(bebida.getId()).orElseThrow().getStockActual());
    }
    @Test void confirmacionesSimultaneasDelMismoTokenSoloIngresanUnaCompra() throws Exception {
        var f=formulario();
        try(var ejecutor=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var tareas=new java.util.ArrayList<java.util.concurrent.Future<Long>>();
            for(int i=0;i<2;i++)tareas.add(ejecutor.submit(()->{
                autenticar();try{return compras.confirmar(f,admin.getNombreUsuario());}
                catch(org.springframework.dao.DataIntegrityViolationException ex){return compras.registrada(f.getToken(),admin.getNombreUsuario()).orElseThrow();}
                finally{SecurityContextHolder.clearContext();}
            }));
            assertEquals(tareas.get(0).get(30,java.util.concurrent.TimeUnit.SECONDS),tareas.get(1).get(30,java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ingresos_mercaderia WHERE token_compra=?",Integer.class,f.getToken()));
        assertEquals(new BigDecimal("10.50"),productos.findById(fruta.getId()).orElseThrow().getStockActual());
        assertEquals(new BigDecimal("12.00"),productos.findById(bebida.getId()).orElseThrow().getStockActual());
    }
    @Test void vistasConfirmacionHttpYReintento() throws Exception {
        var session=new MockHttpSession();session.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(SecurityContextHolder.getContext().getAuthentication()));
        var mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(web.getBean("springSecurityFilterChain",Filter.class)).build();
        var salida=java.nio.file.Path.of("target","vistas-prueba");java.nio.file.Files.createDirectories(salida);
        for(String ruta:new String[]{"/compras","/compras/proveedores","/compras/proveedores/nuevo","/compras/proveedores/"+proveedorId+"/editar"})mvc.perform(get(ruta).session(session)).andExpect(status().isOk());
        var pagina=mvc.perform(get("/compras/nueva").session(session)).andExpect(status().isOk()).andReturn();
        java.nio.file.Files.writeString(salida.resolve("compras-nueva.html"),pagina.getResponse().getContentAsString());
        var csrf=(org.springframework.security.web.csrf.CsrfToken)pagina.getRequest().getAttribute("_csrf");
        String token=(String)session.getAttribute("tokenCompra");
        var peticion=post("/compras/confirmar").session(session).param(csrf.getParameterName(),csrf.getToken()).param("token",token)
                .param("proveedorId",proveedorId.toString()).param("documento","HTTP-"+UUID.randomUUID())
                .param("lineas[0].productoId",fruta.getId().toString()).param("lineas[0].cantidad","0.50").param("lineas[0].costoUnitario","3.33");
        var respuesta=mvc.perform(peticion).andExpect(status().is3xxRedirection()).andReturn();
        String url=respuesta.getResponse().getRedirectedUrl();assertTrue(url.matches("/compras/[0-9]+"));
        mvc.perform(peticion).andExpect(redirectedUrl(url));
        var html=mvc.perform(get(url).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        java.nio.file.Files.writeString(salida.resolve("compras-detalle.html"),html);
        var listado=mvc.perform(get("/compras").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        java.nio.file.Files.writeString(salida.resolve("compras-lista.html"),listado);
        assertEquals(new BigDecimal("10.50"),productos.findById(fruta.getId()).orElseThrow().getStockActual());
    }
}
