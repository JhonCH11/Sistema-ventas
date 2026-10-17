package com.minimarket.stock;

import com.minimarket.productos.domain.*;
import com.minimarket.productos.repository.*;
import com.minimarket.stock.form.*;
import com.minimarket.stock.service.LiquidacionMermaService;
import com.minimarket.usuarios.domain.*;
import com.minimarket.usuarios.repository.UsuarioRepository;
import com.minimarket.usuarios.service.UsuarioAutenticacionService.UsuarioSesion;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.servlet.Filter;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfSystemProperty(named="minimarket.prueba.puerto",matches="[0-9]+")
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:mysql://127.0.0.1:${minimarket.prueba.puerto}/minimarket_liquidacion_prueba?serverTimezone=America/Lima",
    "spring.datasource.username=root", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true", "spring.flyway.clean-disabled=true"})
class LiquidacionMySqlIT {
    @Autowired LiquidacionMermaService service;
    @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias;
    @Autowired UsuarioRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.minimarket.caja.service.CajaService caja;
    @Autowired com.minimarket.ventas.service.VentaService ventas;
    @Autowired WebApplicationContext web;
    @Autowired com.minimarket.stock.service.AlertaStockService alertas;
    @Autowired com.minimarket.importaciones.service.LectorExcelService lector;
    @Autowired com.minimarket.importaciones.service.ImportacionExcelService importador;
    @Autowired com.minimarket.reportes.service.ResumenDiarioService resumenDiario;
    private Producto origen;
    private Usuario admin;
    private LiquidacionForm saldo;
    @BeforeEach void preparar(){
        String id=UUID.randomUUID().toString();
        admin=usuarios.saveAndFlush(new Usuario("Admin prueba","admin-"+id,"hash-prueba",RolUsuario.ADMINISTRADOR));
        var principal=new UsuarioSesion(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"",principal.getAuthorities()));
        var categoria=categorias.saveAndFlush(new Categoria("Frutas "+id));
        origen=productos.saveAndFlush(new Producto(categoria,"Platano "+id,TipoVenta.PESO,new BigDecimal("5.00"),new BigDecimal("10.00"),BigDecimal.ZERO));
        saldo=new LiquidacionForm();saldo.setProductoId(origen.getId());saldo.setToken(UUID.randomUUID().toString());saldo.setCantidad(new BigDecimal("2.00"));saldo.setPrecio(new BigDecimal("3.00"));saldo.setCostoUnitario(new BigDecimal("3.33"));saldo.setMotivo("Maduro");
    }
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();}
    private Long saldoId(){return jdbc.queryForObject("SELECT producto_liquidacion_id FROM liquidaciones_stock WHERE token=?",Long.class,saldo.getToken());}
    private MermaForm merma(Long producto){
        var form=new MermaForm();form.setProductoId(producto);form.setToken(UUID.randomUUID().toString());form.setCantidad(new BigDecimal("0.50"));form.setCostoUnitario(new BigDecimal("99.00"));form.setMotivo(MermaForm.Motivo.DETERIORO);return form;
    }
    @Test void trasladoMermaReintentoYReporteEnMySql(){
        service.liquidar(saldo,admin.getNombreUsuario());service.liquidar(saldo,admin.getNombreUsuario());
        assertEquals(new BigDecimal("8.00"),productos.findById(origen.getId()).orElseThrow().getStockActual());
        var form=merma(saldoId());service.registrarMerma(form,admin.getNombreUsuario());service.registrarMerma(form,admin.getNombreUsuario());
        assertEquals(new BigDecimal("1.50"),productos.findById(saldoId()).orElseThrow().getStockActual());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM mermas_stock WHERE token=?",Integer.class,form.getToken()));
        assertEquals(new BigDecimal("1.67"),jdbc.queryForObject("SELECT perdida FROM mermas_stock WHERE token=?",BigDecimal.class,form.getToken()));
        var hoy=java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"));
        assertTrue(service.mermas(hoy,hoy).stream().anyMatch(m->m.base().equals(origen.getNombre())));
    }
    @Test void falloEnMovimientoRevierteStockYRegistro(){
        var form=merma(origen.getId());form.setCostoUnitario(new BigDecimal("3.33"));
        jdbc.execute("CREATE TRIGGER fallo_merma BEFORE INSERT ON movimientos_stock FOR EACH ROW BEGIN IF NEW.referencia_tipo='MERMA' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo de prueba'; END IF; END");
        try{
            assertThrows(RuntimeException.class,()->service.registrarMerma(form,admin.getNombreUsuario()));
            assertEquals(new BigDecimal("10.00"),productos.findById(origen.getId()).orElseThrow().getStockActual());
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM mermas_stock WHERE token=?",Integer.class,form.getToken()));
        }finally{jdbc.execute("DROP TRIGGER fallo_merma");}
    }
    @Test void falloEnTrasladoReviertePartidaYStock(){
        jdbc.execute("CREATE TRIGGER fallo_liquidacion BEFORE INSERT ON movimientos_stock FOR EACH ROW BEGIN IF NEW.referencia_tipo='LIQUIDACION' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo traslado de prueba'; END IF; END");
        try{
            assertThrows(RuntimeException.class,()->service.liquidar(saldo,admin.getNombreUsuario()));
            assertEquals(new BigDecimal("10.00"),productos.findById(origen.getId()).orElseThrow().getStockActual());
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM liquidaciones_stock WHERE token=?",Integer.class,saldo.getToken()));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM productos WHERE es_liquidacion=1 AND nombre LIKE ?",Integer.class,origen.getNombre()+"%"));
        }finally{jdbc.execute("DROP TRIGGER fallo_liquidacion");}
    }
    @Test void vendeSaldoConPrecioReducidoSinTocarStockNormal() throws Exception {
        service.liquidar(saldo,admin.getNombreUsuario());
        var apertura=new com.minimarket.caja.form.AperturaCajaForm();apertura.setEfectivoInicial(new BigDecimal("100.00"));
        if(caja.buscarAperturaDeHoy().isEmpty())caja.abrirCaja(apertura,admin.getNombreUsuario());
        var carrito=new com.minimarket.ventas.form.CarritoVenta();
        ventas.agregarAlCarrito(carrito,saldoId(),new BigDecimal("0.50"));
        var pago=new com.minimarket.ventas.form.ConfirmarVentaForm();pago.setToken(carrito.getTokenConfirmacion());
        pago.setMedioPago(com.minimarket.ventas.domain.MedioPago.EFECTIVO);pago.setImporteRecibido(new BigDecimal("2.00"));
        var venta=ventas.confirmarVenta(carrito,pago,admin.getNombreUsuario());
        assertEquals(new BigDecimal("1.50"),venta.total());assertEquals(new BigDecimal("0.50"),venta.vuelto());
        assertEquals(new BigDecimal("1.50"),productos.findById(saldoId()).orElseThrow().getStockActual());
        assertEquals(new BigDecimal("8.00"),productos.findById(origen.getId()).orElseThrow().getStockActual());
        int cantidadVentas=jdbc.queryForObject("SELECT COUNT(*) FROM ventas",Integer.class);
        var session=new MockHttpSession();session.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(SecurityContextHolder.getContext().getAuthentication()));
        var mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(web.getBean("springSecurityFilterChain",Filter.class)).build();
        String html="";
        for(int i=0;i<2;i++)html=mvc.perform(get("/ventas/ticket/"+venta.numeroTicket()).session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("formato-impresion"));
        assertEquals(cantidadVentas,jdbc.queryForObject("SELECT COUNT(*) FROM ventas",Integer.class));
        assertEquals(new BigDecimal("1.50"),productos.findById(saldoId()).orElseThrow().getStockActual());
        var salida=java.nio.file.Path.of("target","vistas-prueba");java.nio.file.Files.createDirectories(salida);
        java.nio.file.Files.writeString(salida.resolve("ticket-impresion.html"),html);
    }
    private byte[] excel(com.minimarket.importaciones.form.ImportacionExcel.Tipo tipo,String[]... filas) throws Exception {
        try(var wb=new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(lector.plantilla(tipo)));var out=new java.io.ByteArrayOutputStream()) {
            int numero=1;for(var valores:filas){var row=wb.getSheetAt(0).createRow(numero++);for(int i=0;i<valores.length;i++)row.createCell(i).setCellValue(valores[i]);}wb.write(out);return out.toByteArray();
        }
    }
    @Test void importaProductosSinStockYEntradasConIdempotencia() throws Exception {
        var tipo=com.minimarket.importaciones.form.ImportacionExcel.Tipo.PRODUCTOS;
        String nombre="Mango "+UUID.randomUUID();
        var archivo=excel(tipo,new String[]{nombre,origen.getCategoria().getNombre(),"PESO","5.50","2.00"});
        var previa=importador.preparar(tipo,"Catalogo-"+UUID.randomUUID(),archivo,admin.getNombreUsuario());
        assertTrue(previa.isValida());assertTrue(productos.findByNombreIgnoreCase(nombre).isEmpty());
        importador.confirmar(previa,admin.getNombreUsuario());importador.confirmar(previa,admin.getNombreUsuario());
        assertEquals(0,productos.findByNombreIgnoreCase(nombre).orElseThrow().getStockActual().signum());
        tipo=com.minimarket.importaciones.form.ImportacionExcel.Tipo.STOCK;
        archivo=excel(tipo,new String[]{origen.getNombre(),"1.25","2.33","Ingreso prueba"});
        previa=importador.preparar(tipo,"Compra-"+UUID.randomUUID(),archivo,admin.getNombreUsuario());
        assertTrue(previa.isValida());importador.confirmar(previa,admin.getNombreUsuario());importador.confirmar(previa,admin.getNombreUsuario());
        assertEquals(new BigDecimal("11.25"),productos.findById(origen.getId()).orElseThrow().getStockActual());
        var tipoFinal=tipo;var archivoFinal=archivo;
        assertThrows(IllegalArgumentException.class,()->importador.preparar(tipoFinal,"Otro-"+UUID.randomUUID(),archivoFinal,admin.getNombreUsuario()));
    }
    @Test void errorEnSegundaFilaRevierteTodaLaImportacion() throws Exception {
        var otro=productos.saveAndFlush(new Producto(origen.getCategoria(),"Pera "+UUID.randomUUID(),TipoVenta.PESO,new BigDecimal("5.00"),new BigDecimal("10.00"),BigDecimal.ZERO));
        var tipo=com.minimarket.importaciones.form.ImportacionExcel.Tipo.STOCK;
        var archivo=excel(tipo,new String[]{origen.getNombre(),"1","2","Prueba"},new String[]{otro.getNombre(),"1","2","Prueba"});
        var previa=importador.preparar(tipo,"Rollback-"+UUID.randomUUID(),archivo,admin.getNombreUsuario());assertTrue(previa.isValida());
        jdbc.execute("CREATE TRIGGER fallo_excel BEFORE INSERT ON movimientos_stock FOR EACH ROW BEGIN IF NEW.producto_id="+otro.getId()+" AND NEW.referencia_tipo='INGRESO_MERCADERIA' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo Excel de prueba'; END IF; END");
        try {
            assertThrows(RuntimeException.class,()->importador.confirmar(previa,admin.getNombreUsuario()));
            assertEquals(new BigDecimal("10.00"),productos.findById(origen.getId()).orElseThrow().getStockActual());
            assertEquals(new BigDecimal("10.00"),productos.findById(otro.getId()).orElseThrow().getStockActual());
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM importaciones_excel WHERE token=?",Integer.class,previa.token()));
        } finally {jdbc.execute("DROP TRIGGER fallo_excel");}
    }
    @Test void renderizaResumenExcelYVistaPrevia() throws Exception {
        var session=new MockHttpSession();session.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(SecurityContextHolder.getContext().getAuthentication()));
        var mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(web.getBean("springSecurityFilterChain",Filter.class)).build();
        var htmlResumen=mvc.perform(get("/reportes/resumen-diario").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var resultado=mvc.perform(get("/importaciones/excel").session(session)).andExpect(status().isOk()).andReturn();
        var csrf=(org.springframework.security.web.csrf.CsrfToken)resultado.getRequest().getAttribute("_csrf");
        var archivo=excel(com.minimarket.importaciones.form.ImportacionExcel.Tipo.STOCK,new String[]{origen.getNombre(),"1","2.33","Vista previa"});
        var htmlExcel=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/importaciones/excel/revisar")
                .file(new org.springframework.mock.web.MockMultipartFile("archivo","prueba.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",archivo))
                .param("tipo","STOCK").param("referencia","Vista-"+UUID.randomUUID()).param(csrf.getParameterName(),csrf.getToken()).session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(htmlExcel.contains("Confirmar importaci"));
        var salida=java.nio.file.Path.of("target","vistas-prueba");java.nio.file.Files.createDirectories(salida);
        java.nio.file.Files.writeString(salida.resolve("resumen-diario.html"),htmlResumen);java.nio.file.Files.writeString(salida.resolve("excel-previa.html"),htmlExcel);
    }
    @Test void resumenSeparaReembolsosMermasYCaja() {
        var fecha=java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"));
        var r=resumenDiario.consultar(fecha);
        assertEquals(r.ventas().bruto().subtract(r.ventas().anulado()),r.cobros());
        assertEquals(r.ventas().medios().stream().map(com.minimarket.reportes.service.ReporteService.ResumenMedio::reembolsosPeriodo).reduce(BigDecimal.ZERO,BigDecimal::add),r.reembolsos());
        assertEquals(jdbc.queryForObject("SELECT COALESCE(SUM(perdida),0) FROM mermas_stock WHERE registrada_en>=? AND registrada_en<?",BigDecimal.class,fecha.atStartOfDay(),fecha.plusDays(1).atStartOfDay()),r.mermas());
    }
    @Test void renderizaPantallasYFormulariosConThymeleaf() throws Exception {
        service.liquidar(saldo,admin.getNombreUsuario());
        var session=new MockHttpSession();session.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(SecurityContextHolder.getContext().getAuthentication()));
        var mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(web.getBean("springSecurityFilterChain",Filter.class)).build();
        for(String ruta:new String[]{"/stock/liquidaciones","/stock/liquidaciones/nueva","/stock/liquidaciones/nueva?productoId="+origen.getId(),"/stock/mermas","/stock/mermas/nueva?productoId="+saldoId()}){
            mvc.perform(get(ruta).session(session)).andExpect(status().isOk());
        }
    }
    @Test void alertasRespetanMinimoEstadoYLiquidaciones() {
        origen.setStockMinimo(new BigDecimal("10.00"));productos.saveAndFlush(origen);
        assertTrue(productos.buscarAlertasStock().stream().anyMatch(a->a.id().equals(origen.getId())&&!a.isAgotado()));
        origen.setStockActual(BigDecimal.ZERO);productos.saveAndFlush(origen);
        assertTrue(productos.buscarAlertasStock().stream().anyMatch(a->a.id().equals(origen.getId())&&a.isAgotado()));
        origen.setActivo(false);productos.saveAndFlush(origen);
        assertFalse(productos.buscarAlertasStock().stream().anyMatch(a->a.id().equals(origen.getId())));
        origen.setActivo(true);origen.setStockActual(new BigDecimal("10.00"));origen.setStockMinimo(BigDecimal.ZERO);productos.saveAndFlush(origen);
        assertFalse(productos.buscarAlertasStock().stream().anyMatch(a->a.id().equals(origen.getId())));
        service.liquidar(saldo,admin.getNombreUsuario());
        var liquidado=productos.findById(saldoId()).orElseThrow();liquidado.setStockActual(BigDecimal.ZERO);liquidado.setStockMinimo(BigDecimal.ONE);productos.saveAndFlush(liquidado);
        assertFalse(productos.buscarAlertasStock().stream().anyMatch(a->a.id().equals(liquidado.getId())));
    }
    @Test void renderizaAlertasParaAdministradorYVendedor() throws Exception {
        origen.setNombre("Papaya de prueba");origen.setStockActual(BigDecimal.ZERO);origen.setStockMinimo(new BigDecimal("5.00"));productos.saveAndFlush(origen);
        var sesion=new MockHttpSession();sesion.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(SecurityContextHolder.getContext().getAuthentication()));
        var mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(web.getBean("springSecurityFilterChain",Filter.class)).build();
        var inicio=mvc.perform(get("/").session(sesion)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var lista=mvc.perform(get("/stock/alertas").session(sesion)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(inicio.contains("Papaya de prueba"));assertTrue(lista.contains("Agotado"));
        mvc.perform(get("/stock").param("productoId",origen.getId().toString()).session(sesion)).andExpect(status().isOk());
        var vendedor=usuarios.saveAndFlush(new Usuario("Vendedor prueba","vendedor-"+UUID.randomUUID(),"hash-prueba",RolUsuario.VENDEDOR));
        var principal=new UsuarioSesion(vendedor);
        sesion=new MockHttpSession();sesion.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(new UsernamePasswordAuthenticationToken(principal,"",principal.getAuthorities())));
        mvc.perform(get("/").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/stock/alertas").session(sesion)).andExpect(status().isOk());
        var salida=java.nio.file.Path.of("target","vistas-prueba");java.nio.file.Files.createDirectories(salida);
        java.nio.file.Files.writeString(salida.resolve("inicio-alertas.html"),inicio);
        java.nio.file.Files.writeString(salida.resolve("alertas-stock.html"),lista);
    }
}
