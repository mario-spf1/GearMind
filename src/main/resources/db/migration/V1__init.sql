CREATE TABLE IF NOT EXISTS empresa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    cif VARCHAR(32),
    telefono VARCHAR(32),
    email VARCHAR(150),
    direccion VARCHAR(255),
    ciudad VARCHAR(120),
    provincia VARCHAR(120),
    cp VARCHAR(15),
    activa TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    activo TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_usuario_empresa_email (empresa_id, email),
    CONSTRAINT fk_usuario_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    dni VARCHAR(32),
    email VARCHAR(150),
    telefono VARCHAR(32),
    notas TEXT,
    activo TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cliente_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vehiculo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    matricula VARCHAR(32),
    marca VARCHAR(100),
    modelo VARCHAR(100),
    `year` INT,
    vin VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_vehiculo_empresa (empresa_id),
    KEY idx_vehiculo_cliente (cliente_id),
    CONSTRAINT fk_vehiculo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_vehiculo_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS producto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    referencia VARCHAR(80),
    categoria VARCHAR(80),
    stock INT NOT NULL DEFAULT 0,
    stock_minimo INT NOT NULL DEFAULT 0,
    precio_compra DECIMAL(12, 2),
    precio_venta DECIMAL(12, 2),
    activo TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_producto_empresa (empresa_id),
    CONSTRAINT fk_producto_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS smtp_config (
    empresa_id BIGINT PRIMARY KEY,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    username VARCHAR(255),
    password_encrypted TEXT NOT NULL,
    from_address VARCHAR(255),
    starttls TINYINT NOT NULL DEFAULT 0,
    `ssl` TINYINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_smtp_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mensaje_interno (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    contenido TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    KEY idx_mensaje_empresa (empresa_id),
    KEY idx_mensaje_sender (sender_id),
    KEY idx_mensaje_receiver (receiver_id),
    CONSTRAINT fk_mensaje_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_mensaje_sender FOREIGN KEY (sender_id) REFERENCES usuario(id),
    CONSTRAINT fk_mensaje_receiver FOREIGN KEY (receiver_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fichaje (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    movimiento VARCHAR(20) NOT NULL,
    fecha DATETIME,
    KEY idx_fichaje_empresa (empresa_id),
    KEY idx_fichaje_user (user_id),
    KEY idx_fichaje_fecha (fecha),
    CONSTRAINT fk_fichaje_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_fichaje_user FOREIGN KEY (user_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fichaje_resumen_diario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    minutos_totales INT NOT NULL DEFAULT 0,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fichaje_resumen (empresa_id, user_id, fecha),
    CONSTRAINT fk_fichaje_resumen_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_fichaje_resumen_user FOREIGN KEY (user_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cita (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    empleado_id BIGINT,
    cliente_id BIGINT NOT NULL,
    vehiculo_id BIGINT,
    fecha_hora DATETIME NOT NULL,
    estado VARCHAR(30) NOT NULL,
    origen VARCHAR(30) NOT NULL,
    notas TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_cita_empresa (empresa_id),
    KEY idx_cita_cliente (cliente_id),
    KEY idx_cita_vehiculo (vehiculo_id),
    CONSTRAINT fk_cita_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_cita_empleado FOREIGN KEY (empleado_id) REFERENCES usuario(id) ON DELETE SET NULL,
    CONSTRAINT fk_cita_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_cita_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reparacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cita_id BIGINT,
    cliente_id BIGINT NOT NULL,
    vehiculo_id BIGINT NOT NULL,
    descripcion TEXT,
    estado VARCHAR(30) NOT NULL,
    importe_estimado DECIMAL(12, 2),
    importe_final DECIMAL(12, 2),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    KEY idx_reparacion_empresa (empresa_id),
    KEY idx_reparacion_cliente (cliente_id),
    KEY idx_reparacion_vehiculo (vehiculo_id),
    CONSTRAINT fk_reparacion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_reparacion_cita FOREIGN KEY (cita_id) REFERENCES cita(id) ON DELETE SET NULL,
    CONSTRAINT fk_reparacion_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_reparacion_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS presupuesto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    vehiculo_id BIGINT NOT NULL,
    reparacion_id BIGINT,
    fecha DATETIME NOT NULL,
    estado VARCHAR(30) NOT NULL,
    observaciones TEXT,
    total_estimado DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_presupuesto_empresa (empresa_id),
    CONSTRAINT fk_presupuesto_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_presupuesto_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_presupuesto_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id),
    CONSTRAINT fk_presupuesto_reparacion FOREIGN KEY (reparacion_id) REFERENCES reparacion(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS presupuesto_linea (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    presupuesto_id BIGINT NOT NULL,
    producto_id BIGINT,
    descripcion TEXT,
    cantidad DECIMAL(12, 2) NOT NULL DEFAULT 0,
    precio DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total DECIMAL(12, 2) NOT NULL DEFAULT 0,
    KEY idx_presupuesto_linea_presupuesto (presupuesto_id),
    CONSTRAINT fk_presupuesto_linea_presupuesto FOREIGN KEY (presupuesto_id) REFERENCES presupuesto(id) ON DELETE CASCADE,
    CONSTRAINT fk_presupuesto_linea_producto FOREIGN KEY (producto_id) REFERENCES producto(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS factura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    vehiculo_id BIGINT NOT NULL,
    presupuesto_id BIGINT,
    numero VARCHAR(50) NOT NULL,
    fecha DATETIME NOT NULL,
    estado VARCHAR(30) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0,
    iva DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total DECIMAL(12, 2) NOT NULL DEFAULT 0,
    observaciones TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_factura_empresa (empresa_id),
    CONSTRAINT fk_factura_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_factura_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_factura_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id),
    CONSTRAINT fk_factura_presupuesto FOREIGN KEY (presupuesto_id) REFERENCES presupuesto(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS factura_linea (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    factura_id BIGINT NOT NULL,
    producto_id BIGINT,
    descripcion TEXT,
    cantidad DECIMAL(12, 2) NOT NULL DEFAULT 0,
    precio DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total DECIMAL(12, 2) NOT NULL DEFAULT 0,
    KEY idx_factura_linea_factura (factura_id),
    CONSTRAINT fk_factura_linea_factura FOREIGN KEY (factura_id) REFERENCES factura(id) ON DELETE CASCADE,
    CONSTRAINT fk_factura_linea_producto FOREIGN KEY (producto_id) REFERENCES producto(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pago (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    factura_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    total DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_pagado DECIMAL(12, 2) NOT NULL DEFAULT 0,
    numero_plazos INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_pago_empresa (empresa_id),
    CONSTRAINT fk_pago_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_pago_factura FOREIGN KEY (factura_id) REFERENCES factura(id),
    CONSTRAINT fk_pago_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pago_plazo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pago_id BIGINT NOT NULL,
    numero INT NOT NULL,
    fecha_vencimiento DATE,
    importe DECIMAL(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL,
    fecha_pago DATETIME,
    KEY idx_pago_plazo_pago (pago_id),
    CONSTRAINT fk_pago_plazo_pago FOREIGN KEY (pago_id) REFERENCES pago(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pago_registro (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pago_id BIGINT NOT NULL,
    plazo_id BIGINT,
    fecha DATETIME NOT NULL,
    importe DECIMAL(12, 2) NOT NULL DEFAULT 0,
    observaciones TEXT,
    KEY idx_pago_registro_pago (pago_id),
    CONSTRAINT fk_pago_registro_pago FOREIGN KEY (pago_id) REFERENCES pago(id) ON DELETE CASCADE,
    CONSTRAINT fk_pago_registro_plazo FOREIGN KEY (plazo_id) REFERENCES pago_plazo(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventario_movimiento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    cantidad INT NOT NULL,
    referencia VARCHAR(120),
    notas TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_inventario_empresa (empresa_id),
    KEY idx_inventario_producto (producto_id),
    CONSTRAINT fk_inventario_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_inventario_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tarea (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    orden_trabajo_id BIGINT,
    asignado_a BIGINT,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT,
    estado VARCHAR(30) NOT NULL,
    prioridad VARCHAR(30) NOT NULL,
    fecha_limite DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tarea_empresa (empresa_id),
    CONSTRAINT fk_tarea_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_tarea_reparacion FOREIGN KEY (orden_trabajo_id) REFERENCES reparacion(id) ON DELETE SET NULL,
    CONSTRAINT fk_tarea_usuario FOREIGN KEY (asignado_a) REFERENCES usuario(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS telegram_cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    telegram_chat_id BIGINT NOT NULL,
    telegram_username VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_telegram_cliente_chat (empresa_id, telegram_chat_id),
    UNIQUE KEY uk_telegram_cliente_cliente (empresa_id, cliente_id),
    CONSTRAINT fk_telegram_cliente_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_telegram_cliente_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS telegram_conversacion_estado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    telegram_chat_id BIGINT NOT NULL,
    estado VARCHAR(50) NOT NULL,
    payload TEXT,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_telegram_conversacion_chat (empresa_id, telegram_chat_id),
    CONSTRAINT fk_telegram_conversacion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS telegram_solicitud_cita (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT,
    vehiculo_id BIGINT,
    telegram_chat_id BIGINT NOT NULL,
    mensaje TEXT,
    fecha DATETIME NOT NULL,
    estado VARCHAR(30) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_telegram_solicitud_empresa (empresa_id),
    CONSTRAINT fk_telegram_solicitud_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_telegram_solicitud_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id) ON DELETE SET NULL,
    CONSTRAINT fk_telegram_solicitud_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS telegram_notificacion_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    telegram_chat_id BIGINT NOT NULL,
    tipo_evento VARCHAR(80) NOT NULL,
    payload TEXT,
    enviado_at DATETIME NOT NULL,
    resultado VARCHAR(80) NOT NULL,
    KEY idx_telegram_log_empresa (empresa_id),
    CONSTRAINT fk_telegram_log_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_telegram_log_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
