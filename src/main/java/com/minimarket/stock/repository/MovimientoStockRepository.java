package com.minimarket.stock.repository;

import com.minimarket.stock.domain.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"producto","usuario"})
    java.util.List<MovimientoStock> findTop30ByOrderByIdDesc();
}
