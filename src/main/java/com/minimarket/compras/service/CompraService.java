package com.minimarket.compras.service;

import com.minimarket.compras.form.CompraForm;
import com.minimarket.compras.repository.*;
import com.minimarket.productos.domain.*;
import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.stock.domain.IngresoMercaderia;
import com.minimarket.stock.repository.IngresoMercaderiaRepository;
import com.minimarket.stock.service.StockService;
import com.minimarket.usuarios.service.UsuarioService;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CompraService {
    private final CompraRepository repo;private final ProveedorRepository proveedores;
    private final ProductoRepository productos;private final IngresoMercaderiaRepository ingresos;
    private final StockService stock;private final UsuarioService usuarios;
    public CompraService(CompraRepository repo,ProveedorRepository proveedores,ProductoRepository productos,IngresoMercaderiaRepository ingresos,StockService stock,UsuarioService usuarios){
        this.repo=repo;this.proveedores=proveedores;this.productos=productos;this.ingresos=ingresos;this.stock=stock;this.usuarios=usuarios;
    }
    @Transactional(readOnly=true) public Optional<Long> registrada(String token,String usuario){return repo.registrada(token,usuarios.obtenerUsuarioActivo(usuario).getId());}
    @Transactional(readOnly=true) public List<Producto> productos(){return productos.findByActivoTrueOrderByNombreAsc().stream().filter(p->!p.isLiquidacion()).toList();}
    @Transactional(readOnly=true) public List<CompraRepository.Compra> listar(LocalDate desde,LocalDate hasta){
        if(desde==null||hasta==null||desde.getYear()<1000||hasta.getYear()>9998||hasta.isBefore(desde)||java.time.temporal.ChronoUnit.DAYS.between(desde,hasta)>365)
            throw new IllegalArgumentException("Seleccione un rango valido de hasta 366 dias.");
        return repo.listar(desde,hasta);
    }
    @Transactional(readOnly=true) public CompraRepository.Compra obtener(Long id){return repo.obtener(id).orElseThrow(()->new IllegalArgumentException("Compra no encontrada."));}
    @Transactional(readOnly=true) public List<CompraRepository.Detalle> detalles(Long id){obtener(id);return repo.detalles(id);}

    @Transactional public Long confirmar(CompraForm form,String nombreUsuario){
        var usuario=usuarios.obtenerUsuarioActivo(nombreUsuario);
        if(form.getToken()==null)throw new IllegalArgumentException("Abra nuevamente Nueva compra.");
        try{UUID.fromString(form.getToken());}catch(IllegalArgumentException ex){throw new IllegalArgumentException("Abra nuevamente Nueva compra.");}
        var existente=repo.registrada(form.getToken(),usuario.getId());if(existente.isPresent())return existente.get();
        if(form.getLineas()==null||form.getLineas().isEmpty()||form.getLineas().size()>50)throw new IllegalArgumentException("Agregue entre 1 y 50 productos.");
        String documento=opcional(form.getDocumento(),100),observacion=opcional(form.getObservacion(),300);
        if(documento!=null&&form.getProveedorId()==null)throw new IllegalArgumentException("Seleccione proveedor para registrar el numero de documento.");
        var proveedor=form.getProveedorId()==null?null:proveedores.obtener(form.getProveedorId(),true).filter(ProveedorRepository.Proveedor::activo)
                .orElseThrow(()->new IllegalArgumentException("El proveedor ya no esta activo."));
        var ids=new TreeSet<Long>();
        BigDecimal total=BigDecimal.ZERO;
        for(var f:form.getLineas()){
            if(f==null||f.getProductoId()==null||!ids.add(f.getProductoId()))throw new IllegalArgumentException("Seleccione cada producto una sola vez.");
            validarImporte(f.getCantidad());validarImporte(f.getCostoUnitario());
            total=total.add(subtotal(f.getCantidad(),f.getCostoUnitario()));
        }
        validarImporte(total);
        Map<Long,Producto> bloqueados=productos.findAllByIdInForUpdate(ids).stream().collect(Collectors.toMap(Producto::getId,Function.identity()));
        for(var f:form.getLineas()){
            var p=bloqueados.get(f.getProductoId());
            if(p==null||!p.isActivo()||p.isLiquidacion())throw new IllegalArgumentException("Un producto no esta disponible. Revise la compra.");
            if(p.getTipoVenta()==TipoVenta.UNIDAD&&f.getCantidad().stripTrailingZeros().scale()>0)throw new IllegalArgumentException("Ingrese unidades enteras para "+p.getNombre());
        }
        var ingreso=new IngresoMercaderia(usuario,observacion);
        ingreso.datosCompra(form.getProveedorId(),proveedor==null?"Sin proveedor":proveedor.nombre(),form.getToken(),documento,total);
        ingresos.saveAndFlush(ingreso);
        for(var f:form.getLineas())stock.registrarDetalle(ingreso,f.getProductoId(),f.getCantidad(),f.getCostoUnitario());
        ingresos.flush();return ingreso.getId();
    }
    public static BigDecimal subtotal(BigDecimal cantidad,BigDecimal costo){return cantidad.multiply(costo).setScale(2,RoundingMode.HALF_UP);}
    private static void validarImporte(BigDecimal v){
        if(v==null||v.signum()<=0||v.stripTrailingZeros().scale()>2||v.compareTo(new BigDecimal("9999999999.99"))>0)
            throw new IllegalArgumentException("Cantidades, costos y total deben ser positivos, con hasta dos decimales y diez cifras enteras.");
    }
    private static String opcional(String valor,int max){if(valor==null||valor.isBlank())return null;if(valor.length()>max)throw new IllegalArgumentException("Un texto supera la longitud permitida.");return valor.strip();}
}
