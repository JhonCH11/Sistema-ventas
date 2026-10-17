package com.minimarket.productos.repository;

import com.minimarket.productos.domain.Producto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("""
            select new com.minimarket.stock.form.AlertaStock(p.id,p.nombre,p.tipoVenta,p.stockActual,p.stockMinimo)
            from Producto p where p.activo=true and p.liquidacion=false and p.stockActual<=p.stockMinimo
            order by case when p.stockActual=0 then 0 else 1 end,p.nombre,p.id
            """)
    List<com.minimarket.stock.form.AlertaStock> buscarAlertasStock();

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findByNombreIgnoreCase(String nombre);

    @Override
    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select producto from Producto producto where producto.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select producto from Producto producto where producto.id in :ids order by producto.id")
    List<Producto> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);
}
