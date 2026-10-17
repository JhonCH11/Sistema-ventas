ALTER TABLE productos
    ADD COLUMN es_liquidacion BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN precio_referencia_liquidacion DECIMAL(12,2) NULL;

CREATE TABLE liquidaciones_stock (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token CHAR(36) NOT NULL UNIQUE,
    producto_origen_id BIGINT UNSIGNED NOT NULL,
    producto_liquidacion_id BIGINT UNSIGNED NULL UNIQUE,
    usuario_id BIGINT UNSIGNED NOT NULL,
    cantidad_inicial DECIMAL(12,2) NOT NULL,
    costo_unitario DECIMAL(12,2) NOT NULL,
    fecha_llegada DATE NULL,
    motivo VARCHAR(200) NOT NULL,
    registrada_en DATETIME(3) NOT NULL,
    FOREIGN KEY (producto_origen_id) REFERENCES productos(id),
    FOREIGN KEY (producto_liquidacion_id) REFERENCES productos(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CHECK (cantidad_inicial > 0 AND costo_unitario > 0)
);

CREATE TABLE mermas_stock (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token CHAR(36) NOT NULL UNIQUE,
    producto_id BIGINT UNSIGNED NOT NULL,
    usuario_id BIGINT UNSIGNED NOT NULL,
    producto_nombre VARCHAR(150) NOT NULL,
    producto_base_nombre VARCHAR(150) NOT NULL,
    tipo_venta VARCHAR(10) NOT NULL,
    cantidad DECIMAL(12,2) NOT NULL,
    costo_unitario DECIMAL(12,2) NOT NULL,
    perdida DECIMAL(12,2) NOT NULL,
    motivo VARCHAR(30) NOT NULL,
    observacion VARCHAR(200) NULL,
    registrada_en DATETIME(3) NOT NULL,
    KEY idx_mermas_fecha (registrada_en),
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CHECK (cantidad > 0 AND costo_unitario > 0 AND perdida >= 0)
);
