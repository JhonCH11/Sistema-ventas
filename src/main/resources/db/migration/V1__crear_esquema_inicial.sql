


















CREATE TABLE configuracion_negocio (
    id TINYINT UNSIGNED NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    direccion VARCHAR(200) NULL,
    telefono VARCHAR(30) NULL,
    ruta_respaldos VARCHAR(500) NULL,
    creado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    actualizado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT chk_configuracion_unica CHECK (id = 1)
);

INSERT INTO configuracion_negocio (id, nombre) VALUES (1, 'Mi minimarket');

CREATE TABLE usuarios (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    nombre_usuario VARCHAR(60) NOT NULL,
    contrasena_hash VARCHAR(100) NOT NULL,
    rol ENUM('ADMINISTRADOR', 'VENDEDOR') NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),


    actualizado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuarios_nombre_usuario (nombre_usuario)
);

CREATE TABLE categorias (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    actualizado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_categorias_nombre (nombre)
);

CREATE TABLE proveedores (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    telefono VARCHAR(30) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    actualizado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_proveedores_nombre (nombre)
);

CREATE TABLE productos (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    categoria_id BIGINT UNSIGNED NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    tipo_venta ENUM('UNIDAD', 'PESO') NOT NULL,
    precio_actual DECIMAL(12, 2) NOT NULL,
    stock_actual DECIMAL(12, 3) NOT NULL DEFAULT 0.000,
    stock_minimo DECIMAL(12, 3) NOT NULL DEFAULT 0.000,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    actualizado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_productos_nombre (nombre),
    KEY idx_productos_categoria (categoria_id),
    KEY idx_productos_activo_nombre (activo, nombre),
    CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT chk_productos_precio_positivo CHECK (precio_actual > 0),
    CONSTRAINT chk_productos_stock_no_negativo CHECK (stock_actual >= 0),
    CONSTRAINT chk_productos_stock_minimo_no_negativo CHECK (stock_minimo >= 0)
);

CREATE TABLE historial_precios (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    producto_id BIGINT UNSIGNED NOT NULL,
    usuario_id BIGINT UNSIGNED NOT NULL,
    precio_anterior DECIMAL(12, 2) NOT NULL,
    precio_nuevo DECIMAL(12, 2) NOT NULL,
    cambiado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_historial_precios_producto_fecha (producto_id, cambiado_en),
    CONSTRAINT fk_historial_precios_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_historial_precios_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT chk_historial_precios_anterior_positivo CHECK (precio_anterior > 0),
    CONSTRAINT chk_historial_precios_nuevo_positivo CHECK (precio_nuevo > 0)
);

CREATE TABLE aperturas_caja (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT UNSIGNED NOT NULL,
    fecha_operacion DATE NOT NULL,
    efectivo_inicial DECIMAL(12, 2) NOT NULL,
    estado ENUM('ABIERTA', 'CERRADA') NOT NULL DEFAULT 'ABIERTA',
    abierta_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_aperturas_caja_fecha (fecha_operacion),
    CONSTRAINT fk_aperturas_caja_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT chk_aperturas_caja_efectivo_no_negativo CHECK (efectivo_inicial >= 0)
);

CREATE TABLE ingresos_mercaderia (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    proveedor_id BIGINT UNSIGNED NULL,
    usuario_id BIGINT UNSIGNED NOT NULL,
    recibido_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    observacion VARCHAR(300) NULL,
    PRIMARY KEY (id),
    KEY idx_ingresos_mercaderia_fecha (recibido_en),
    CONSTRAINT fk_ingresos_mercaderia_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_ingresos_mercaderia_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE ingreso_detalles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ingreso_mercaderia_id BIGINT UNSIGNED NOT NULL,
    producto_id BIGINT UNSIGNED NOT NULL,
    cantidad DECIMAL(12, 3) NOT NULL,
    costo_unitario DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ingreso_detalles_ingreso (ingreso_mercaderia_id),
    CONSTRAINT fk_ingreso_detalles_ingreso FOREIGN KEY (ingreso_mercaderia_id) REFERENCES ingresos_mercaderia (id),
    CONSTRAINT fk_ingreso_detalles_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT chk_ingreso_detalles_cantidad_positiva CHECK (cantidad > 0),
    CONSTRAINT chk_ingreso_detalles_costo_positivo CHECK (costo_unitario > 0)
);

CREATE TABLE ventas (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    apertura_caja_id BIGINT UNSIGNED NOT NULL,
    vendedor_id BIGINT UNSIGNED NOT NULL,
    numero_ticket VARCHAR(30) NOT NULL,
    estado ENUM('CONFIRMADA', 'ANULADA') NOT NULL DEFAULT 'CONFIRMADA',
    total DECIMAL(12, 2) NOT NULL,
    vendida_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ventas_numero_ticket (numero_ticket),
    KEY idx_ventas_fecha_estado (vendida_en, estado),
    KEY idx_ventas_apertura_caja (apertura_caja_id),
    CONSTRAINT fk_ventas_apertura_caja FOREIGN KEY (apertura_caja_id) REFERENCES aperturas_caja (id),
    CONSTRAINT fk_ventas_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios (id),
    CONSTRAINT chk_ventas_total_no_negativo CHECK (total >= 0)
);

CREATE TABLE venta_detalles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    venta_id BIGINT UNSIGNED NOT NULL,
    producto_id BIGINT UNSIGNED NOT NULL,
    producto_nombre VARCHAR(150) NOT NULL,
    tipo_venta ENUM('UNIDAD', 'PESO') NOT NULL,
    cantidad DECIMAL(12, 3) NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_venta_detalles_venta (venta_id),
    KEY idx_venta_detalles_producto (producto_id),
    CONSTRAINT fk_venta_detalles_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    CONSTRAINT fk_venta_detalles_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT chk_venta_detalles_cantidad_positiva CHECK (cantidad > 0),
    CONSTRAINT chk_venta_detalles_precio_positivo CHECK (precio_unitario > 0),
    CONSTRAINT chk_venta_detalles_subtotal_no_negativo CHECK (subtotal >= 0),
    CONSTRAINT chk_venta_detalles_unidades_enteras CHECK (
        tipo_venta <> 'UNIDAD' OR cantidad = FLOOR(cantidad)
    )
);

CREATE TABLE pagos (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    venta_id BIGINT UNSIGNED NOT NULL,
    medio_pago ENUM('EFECTIVO', 'YAPE', 'PLIN') NOT NULL,
    importe_pagado DECIMAL(12, 2) NOT NULL,
    importe_recibido DECIMAL(12, 2) NULL,
    vuelto DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    pagado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_pagos_venta (venta_id),
    CONSTRAINT fk_pagos_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    CONSTRAINT chk_pagos_importe_positivo CHECK (importe_pagado > 0),
    CONSTRAINT chk_pagos_recibido_no_negativo CHECK (importe_recibido IS NULL OR importe_recibido >= 0),
    CONSTRAINT chk_pagos_vuelto_no_negativo CHECK (vuelto >= 0),
    CONSTRAINT chk_pagos_efectivo_recibido CHECK (
        medio_pago <> 'EFECTIVO' OR importe_recibido >= importe_pagado
    ),
    CONSTRAINT chk_pagos_digital_sin_vuelto CHECK (
        medio_pago = 'EFECTIVO' OR (importe_recibido IS NULL AND vuelto = 0)
    )
);

CREATE TABLE anulaciones_venta (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    venta_id BIGINT UNSIGNED NOT NULL,
    administrador_id BIGINT UNSIGNED NOT NULL,
    motivo ENUM('ERROR_REGISTRO', 'CLIENTE_CANCELO', 'PAGO_NO_CONFIRMADO') NOT NULL,
    observacion VARCHAR(300) NULL,
    anulada_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_anulaciones_venta (venta_id),
    CONSTRAINT fk_anulaciones_venta_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    CONSTRAINT fk_anulaciones_venta_administrador FOREIGN KEY (administrador_id) REFERENCES usuarios (id)
);

CREATE TABLE devoluciones (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    venta_id BIGINT UNSIGNED NOT NULL,
    administrador_id BIGINT UNSIGNED NOT NULL,
    motivo ENUM('PRODUCTO_MAL_ESTADO', 'PRODUCTO_EQUIVOCADO', 'CLIENTE_CAMBIO_OPINION') NOT NULL,
    observacion VARCHAR(300) NULL,
    devuelta_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_devoluciones_venta_fecha (venta_id, devuelta_en),
    CONSTRAINT fk_devoluciones_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    CONSTRAINT fk_devoluciones_administrador FOREIGN KEY (administrador_id) REFERENCES usuarios (id)
);

CREATE TABLE devolucion_detalles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    devolucion_id BIGINT UNSIGNED NOT NULL,
    venta_detalle_id BIGINT UNSIGNED NOT NULL,
    cantidad_devuelta DECIMAL(12, 3) NOT NULL,
    importe_devuelto DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_devolucion_detalles_devolucion (devolucion_id),
    CONSTRAINT fk_devolucion_detalles_devolucion FOREIGN KEY (devolucion_id) REFERENCES devoluciones (id),
    CONSTRAINT fk_devolucion_detalles_venta_detalle FOREIGN KEY (venta_detalle_id) REFERENCES venta_detalles (id),
    CONSTRAINT chk_devolucion_detalles_cantidad_positiva CHECK (cantidad_devuelta > 0),
    CONSTRAINT chk_devolucion_detalles_importe_no_negativo CHECK (importe_devuelto >= 0)
);

CREATE TABLE movimientos_stock (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    producto_id BIGINT UNSIGNED NOT NULL,
    usuario_id BIGINT UNSIGNED NOT NULL,
    tipo ENUM(
        'STOCK_INICIAL',
        'INGRESO_MERCADERIA',
        'VENTA',
        'ANULACION',
        'DEVOLUCION',
        'AJUSTE_POSITIVO',
        'AJUSTE_NEGATIVO'
    ) NOT NULL,
    cantidad DECIMAL(12, 3) NOT NULL,
    motivo VARCHAR(100) NULL,
    observacion VARCHAR(300) NULL,
    referencia_tipo VARCHAR(50) NULL,
    referencia_id BIGINT UNSIGNED NULL,
    registrado_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_movimientos_stock_producto_fecha (producto_id, registrado_en),
    KEY idx_movimientos_stock_referencia (referencia_tipo, referencia_id),
    CONSTRAINT fk_movimientos_stock_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_movimientos_stock_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT chk_movimientos_stock_cantidad_no_cero CHECK (cantidad <> 0)
);

CREATE TABLE cierres_caja (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    apertura_caja_id BIGINT UNSIGNED NOT NULL,
    usuario_id BIGINT UNSIGNED NOT NULL,
    efectivo_esperado DECIMAL(12, 2) NOT NULL,
    efectivo_contado DECIMAL(12, 2) NOT NULL,
    diferencia DECIMAL(12, 2) NOT NULL,
    observacion VARCHAR(300) NULL,
    cerrada_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_cierres_caja_apertura (apertura_caja_id),
    CONSTRAINT fk_cierres_caja_apertura FOREIGN KEY (apertura_caja_id) REFERENCES aperturas_caja (id),
    CONSTRAINT fk_cierres_caja_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);
