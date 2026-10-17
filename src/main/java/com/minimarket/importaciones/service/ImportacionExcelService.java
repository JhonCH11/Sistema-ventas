package com.minimarket.importaciones.service;

import com.minimarket.importaciones.form.ImportacionExcel.*;
import com.minimarket.importaciones.repository.ImportacionExcelRepository;
import com.minimarket.productos.domain.*;
import com.minimarket.productos.form.ProductoForm;
import com.minimarket.productos.repository.*;
import com.minimarket.productos.service.ProductoService;
import com.minimarket.stock.form.IngresoStockForm;
import com.minimarket.stock.service.StockService;
import com.minimarket.usuarios.service.UsuarioService;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ImportacionExcelService {
    private final LectorExcelService lector;private final ProductoRepository productos;private final CategoriaRepository categorias;
    private final ProductoService catalogo;private final StockService stock;private final UsuarioService usuarios;private final ImportacionExcelRepository registros;
    public ImportacionExcelService(LectorExcelService lector,ProductoRepository productos,CategoriaRepository categorias,ProductoService catalogo,StockService stock,UsuarioService usuarios,ImportacionExcelRepository registros) {
        this.lector=lector;this.productos=productos;this.categorias=categorias;this.catalogo=catalogo;this.stock=stock;this.usuarios=usuarios;this.registros=registros;
    }
    @Transactional(readOnly=true)
    public Previa preparar(Tipo tipo,String referencia,byte[] bytes,String usuario) {
        if(referencia==null||referencia.isBlank()||referencia.length()>100)throw new IllegalArgumentException("Indique una referencia unica de hasta 100 caracteres para esta carga.");
        String hash;
        try{hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception ex){throw new IllegalStateException(ex);}
        if(registros.duplicada(tipo.name(),referencia.strip(),hash))throw new IllegalArgumentException("Este archivo o referencia ya se importo. No se duplicara la carga.");
        var usuarioId=usuarios.obtenerUsuarioActivo(usuario).getId();var leidas=lector.leer(tipo,bytes);List<Fila> filas=new ArrayList<>();Set<String> nombres=new HashSet<>();
        for(var f:leidas) {
            var errores=new ArrayList<>(f.errores());Long productoId=null,categoriaId=null;TipoVenta venta=f.tipo();String nombre=f.nombre();
            if(!nombres.add(Normalizer.normalize(f.nombre(),Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)))errores.add("Producto repetido en el archivo.");
            if(errores.isEmpty()) {
                if(tipo==Tipo.PRODUCTOS) {
                    if(productos.findByNombreIgnoreCase(f.nombre()).isPresent())errores.add("El producto ya existe; no se modificara.");
                    var categoria=categorias.findByNombreIgnoreCase(f.categoria()).filter(Categoria::isActivo);
                    if(categoria.isEmpty())errores.add("La categoria no existe o esta inactiva. Registrela antes de importar.");else categoriaId=categoria.get().getId();
                } else {
                    var p=productos.findByNombreIgnoreCase(f.nombre()).filter(Producto::isActivo).filter(x->!x.isLiquidacion());
                    if(p.isEmpty())errores.add("Producto no encontrado, inactivo o en liquidacion.");
                    else {productoId=p.get().getId();nombre=p.get().getNombre();venta=p.get().getTipoVenta();if(venta==TipoVenta.UNIDAD&&f.cantidad().stripTrailingZeros().scale()>0)errores.add("Ingrese unidades enteras.");}
                }
            }
            filas.add(new Fila(f.numero(),nombre,f.categoria(),venta,f.precio(),f.minimo(),f.cantidad(),f.costo(),f.observacion(),productoId,categoriaId,List.copyOf(errores)));
        }
        return new Previa(UUID.randomUUID().toString(),tipo,referencia.strip(),hash,usuarioId,Instant.now(),List.copyOf(filas));
    }
    @Transactional(readOnly=true)
    public boolean registrada(String token,String usuario) {return registros.registrada(token,usuarios.obtenerUsuarioActivo(usuario).getId());}

    @Transactional
    public void confirmar(Previa previa,String usuario) {
        var actual=usuarios.obtenerUsuarioActivo(usuario);
        if(!Objects.equals(actual.getId(),previa.usuarioId()))throw new IllegalArgumentException("La vista previa pertenece a otra persona.");
        if(registros.registrada(previa.token(),actual.getId()))return;
        if(!previa.isValida()||previa.creada().isBefore(Instant.now().minus(Duration.ofMinutes(30))))throw new IllegalArgumentException("La vista previa contiene errores o caduco. Cargue el archivo nuevamente.");
        registros.registrar(previa);
        Map<Long,Producto> bloqueados=Map.of();
        if(previa.tipo()==Tipo.STOCK) {
            var ids=previa.filas().stream().map(Fila::productoId).sorted().toList();
            bloqueados=productos.findAllByIdInForUpdate(ids).stream().collect(Collectors.toMap(Producto::getId,Function.identity()));
        }
        for(var f:previa.filas()) {
            if(previa.tipo()==Tipo.PRODUCTOS) {
                var c=categorias.findById(f.categoriaId()).filter(Categoria::isActivo).orElseThrow(()->new IllegalArgumentException("Una categoria dejo de estar disponible. Cargue nuevamente el archivo."));
                var form=new ProductoForm();form.setNombre(f.nombre());form.setCategoriaId(c.getId());form.setTipoVenta(f.tipo());form.setPrecioActual(f.precio());form.setStockMinimo(f.minimo());form.setStockInicial(BigDecimal.ZERO);catalogo.crearProducto(form,usuario);
            } else {
                var p=bloqueados.get(f.productoId());
                if(p==null||!p.isActivo()||p.isLiquidacion()||p.getTipoVenta()!=f.tipo()||!p.getNombre().equalsIgnoreCase(f.nombre()))throw new IllegalArgumentException("Un producto cambio desde la vista previa. Cargue nuevamente el archivo.");
                var nuevo=p.getStockActual().add(f.cantidad());if(nuevo.precision()-nuevo.scale()>10)throw new IllegalArgumentException("El stock resultante es demasiado grande: "+p.getNombre());
                var form=new IngresoStockForm();form.setProductoId(p.getId());form.setCantidad(f.cantidad());form.setCostoUnitario(f.costo());form.setObservacion("Excel "+previa.referencia()+" - "+f.observacion());stock.registrarIngreso(form,usuario);
            }
        }
        productos.flush();
    }
}
