CREATE TABLE importaciones_excel (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token CHAR(36) NOT NULL UNIQUE,
    tipo VARCHAR(10) NOT NULL,
    referencia VARCHAR(100) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    usuario_id BIGINT UNSIGNED NOT NULL,
    filas INT NOT NULL,
    importada_en DATETIME(3) NOT NULL,
    UNIQUE KEY uk_importacion_referencia (tipo,referencia),
    UNIQUE KEY uk_importacion_archivo (tipo,sha256),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CHECK (filas>0 AND filas<=500),
    CHECK (tipo IN ('PRODUCTOS','STOCK'))
);
