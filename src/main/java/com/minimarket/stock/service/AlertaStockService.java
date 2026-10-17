package com.minimarket.stock.service;

import com.minimarket.productos.repository.ProductoRepository;
import com.minimarket.stock.form.AlertaStock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertaStockService {
    private final ProductoRepository productos;
    public AlertaStockService(ProductoRepository productos) { this.productos=productos; }
    @Transactional(readOnly=true)
    public Resumen consultar() {
        var alertas=List.copyOf(productos.buscarAlertasStock());
        long agotados=alertas.stream().filter(AlertaStock::isAgotado).count();
        return new Resumen(agotados,alertas.size()-agotados,alertas);
    }
    public record Resumen(long agotados,long bajos,List<AlertaStock> productos) {
        public int getTotal() { return productos.size(); }
        public List<AlertaStock> getDestacados() { return productos.stream().limit(6).toList(); }
    }
}
