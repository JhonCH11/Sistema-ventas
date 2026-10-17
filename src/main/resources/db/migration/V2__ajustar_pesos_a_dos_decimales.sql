ALTER TABLE productos
    MODIFY COLUMN stock_actual DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN stock_minimo DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE ingreso_detalles
    MODIFY COLUMN cantidad DECIMAL(12, 2) NOT NULL;

ALTER TABLE venta_detalles
    MODIFY COLUMN cantidad DECIMAL(12, 2) NOT NULL;

ALTER TABLE devolucion_detalles
    MODIFY COLUMN cantidad_devuelta DECIMAL(12, 2) NOT NULL;

ALTER TABLE movimientos_stock
    MODIFY COLUMN cantidad DECIMAL(12, 2) NOT NULL;
