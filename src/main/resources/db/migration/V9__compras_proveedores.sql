ALTER TABLE ingresos_mercaderia
    ADD COLUMN token_compra CHAR(36) NULL,
    ADD COLUMN documento VARCHAR(100) NULL,
    ADD COLUMN proveedor_nombre VARCHAR(150) NULL,
    ADD COLUMN total_compra DECIMAL(12,2) NULL,
    ADD UNIQUE KEY uk_ingresos_token_compra (token_compra),
    ADD UNIQUE KEY uk_ingresos_proveedor_documento (proveedor_id, documento),
    ADD CONSTRAINT chk_ingresos_total_compra CHECK (total_compra IS NULL OR total_compra > 0);

ALTER TABLE ingreso_detalles
    ADD COLUMN producto_nombre VARCHAR(150) NULL,
    ADD COLUMN tipo_venta ENUM('UNIDAD','PESO') NULL;
