CREATE TABLE movimientos_dinero_caja (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    apertura_caja_id BIGINT UNSIGNED NOT NULL,
    usuario_id BIGINT UNSIGNED NOT NULL,
    token CHAR(36) NOT NULL UNIQUE,
    tipo VARCHAR(10) NOT NULL,
    importe DECIMAL(12,2) NOT NULL,
    impacto DECIMAL(12,2) NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    registrado_en DATETIME(3) NOT NULL,
    reversion_de_id BIGINT UNSIGNED NULL UNIQUE,
    KEY idx_movimientos_dinero_apertura (apertura_caja_id,id),
    FOREIGN KEY (apertura_caja_id) REFERENCES aperturas_caja(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (reversion_de_id) REFERENCES movimientos_dinero_caja(id),
    CHECK (importe > 0),
    CHECK (tipo IN ('INGRESO','RETIRO','GASTO')),
    CHECK (impacto = importe OR impacto = -importe)
);
