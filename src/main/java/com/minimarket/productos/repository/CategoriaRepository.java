package com.minimarket.productos.repository;

import com.minimarket.productos.domain.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByActivoTrueOrderByNombreAsc();

    Optional<Categoria> findByNombreIgnoreCase(String nombre);
}
