-- ==============================================================================
-- Script de Generación de Datos de Prueba - CineZone (ON CONFLICT DO NOTHING)
-- Ejecutar manualmente en pgAdmin / DBeaver / Datagrip.
-- ¡ATENCIÓN!: Spring Boot no lo ejecuta automáticamente, debes correrlo a mano.
-- ==============================================================================

DO $$
DECLARE
    -- Variables para IDs de niveles
    v_id_azul BIGINT;
    v_id_dorado BIGINT;
    v_id_negro BIGINT;

    -- Arreglos de nombres y apellidos peruanos
    v_nombres TEXT[] := ARRAY['Luis', 'Carlos', 'Miguel', 'Jose', 'Jorge', 'Juan', 'Pedro', 'Ricardo', 'Fernando', 'Roberto', 'Diego', 'Alejandro', 'Eduardo', 'Manuel', 'Daniel', 'Julio', 'Victor', 'Mario', 'Hugo', 'Oscar', 'Maria', 'Ana', 'Carmen', 'Rosa', 'Marta', 'Julia', 'Silvia', 'Teresa', 'Elena', 'Laura', 'Isabel', 'Patricia', 'Monica', 'Sofia', 'Lucia', 'Paula', 'Andrea', 'Claudia', 'Valeria', 'Daniela'];
    v_apellidos TEXT[] := ARRAY['Garcia', 'Flores', 'Sanchez', 'Perez', 'Rodriguez', 'Gomez', 'Diaz', 'Vasquez', 'Cruz', 'Ramirez', 'Rojas', 'Chavez', 'Quispe', 'Gonzales', 'Torres', 'Fernandez', 'Gutierez', 'Mendoza', 'Ruiz', 'Silva', 'Ramos', 'Vargas', 'Rios', 'Navarro', 'Castro', 'Soto', 'Herrera', 'Espinoza', 'Mejia', 'Huaman', 'Castillo', 'Reyes', 'Campos', 'Leon', 'Vega', 'Caballero', 'Maldonado', 'Guerrero', 'Aguilar', 'Paredes'];

    -- Variables temporales para usuarios
    i INT;
    v_idx_nombre INT;
    v_idx_apellido INT;
    v_nombre TEXT;
    v_apellido TEXT;
    v_correo TEXT;
    v_celular TEXT;
    v_fecha_nacimiento DATE;
    v_dni TEXT;
    v_genero TEXT;
    v_nivel_id BIGINT;
    v_puntos INT;
    v_yearly_visits INT;
    v_consumo DECIMAL(10,2);
    v_mes_beneficio INT := EXTRACT(MONTH FROM CURRENT_DATE)::int;
    v_fecha_registro TIMESTAMP;
    v_fecha_inicio_periodo DATE;
    v_fecha_ultima_visita DATE;
    v_nivel_texto TEXT;
    v_usuario_id UUID;

    -- Boletas
    b INT;
    v_num_boletas INT;
    v_boleta_id UUID;
    v_funcion_id BIGINT;
    v_sala_id BIGINT;
    v_monto_total DECIMAL(10,2);
    v_fecha_compra TIMESTAMP;

    -- Tickets
    t INT;
    v_num_tickets INT;
    v_precio_ticket DECIMAL(10,2);
    v_beneficio_id BIGINT;
    v_asiento_id BIGINT;

    -- Dulcería
    d INT;
    v_num_dulces INT;
    v_producto_id BIGINT;
    v_cantidad INT;
    v_precio_unitario DECIMAL(10,2);
    v_precio_total DECIMAL(10,2);

    -- Arrays de funciones activas
    v_funciones_ids BIGINT[];

BEGIN
    -- Obtener IDs de niveles
    SELECT id_nivel INTO v_id_azul FROM niveles_tarjeta WHERE UPPER(nombre) = 'AZUL';
    SELECT id_nivel INTO v_id_dorado FROM niveles_tarjeta WHERE UPPER(nombre) = 'DORADO';
    SELECT id_nivel INTO v_id_negro FROM niveles_tarjeta WHERE UPPER(nombre) = 'NEGRO';

    -- Obtener funciones válidas (Peliculas 7, 8, 9) que estén activas
    SELECT ARRAY(
        SELECT id FROM funciones 
        WHERE pelicula_id IN (7, 8, 9) AND activa = true
    ) INTO v_funciones_ids;

    -- Tablas temporales
    CREATE TEMP TABLE IF NOT EXISTS temp_usuarios (id UUID, nivel TEXT);
    CREATE TEMP TABLE IF NOT EXISTS temp_boletas (id UUID, usuario_id UUID, nivel TEXT, funcion_id BIGINT);

    TRUNCATE temp_usuarios;
    TRUNCATE temp_boletas;

    -- ==========================================
    -- 1. Insertar 60 usuarios
    -- ==========================================
    FOR i IN 1..60 LOOP
        v_idx_nombre := floor(random() * array_length(v_nombres, 1) + 1);
        v_idx_apellido := floor(random() * array_length(v_apellidos, 1) + 1);
        v_nombre := v_nombres[v_idx_nombre];
        v_apellido := v_apellidos[v_idx_apellido];
        v_correo := lower(v_nombre || '.' || v_apellido || i || '@gmail.com');
        v_celular := '9' || floor(random() * 90000000 + 10000000)::text;
        v_fecha_nacimiento := DATE '1980-01-01' + (floor(random() * 8760)::int);
        v_dni := lpad((floor(random() * 90000000 + 10000000))::text, 8, '0');
        v_genero := CASE WHEN random() > 0.5 THEN 'MASCULINO' ELSE 'FEMENINO' END;
        v_fecha_registro := NOW() - (floor(random() * 365 + 30) || ' days')::interval;
        v_fecha_inicio_periodo := CURRENT_DATE - (floor(random() * 270 + 30) || ' days')::interval;
        v_fecha_ultima_visita := CURRENT_DATE - (floor(random() * 14 + 1) || ' days')::interval;
        v_usuario_id := gen_random_uuid();

        -- Distribución de Niveles: 20 AZUL, 20 DORADO, 20 NEGRO
        IF i <= 20 THEN
            v_nivel_id := v_id_azul;
            v_nivel_texto := 'AZUL';
            v_puntos := floor(random() * 151 + 50);
            v_yearly_visits := floor(random() * 6 + 3);
            v_consumo := (random() * 60 + 20)::decimal(10,2);
        ELSIF i <= 40 THEN
            v_nivel_id := v_id_dorado;
            v_nivel_texto := 'DORADO';
            v_puntos := floor(random() * 300 + 201);
            v_yearly_visits := floor(random() * 10 + 9);
            v_consumo := (random() * 119 + 81)::decimal(10,2);
        ELSE
            v_nivel_id := v_id_negro;
            v_nivel_texto := 'NEGRO';
            v_puntos := floor(random() * 500 + 501);
            v_yearly_visits := floor(random() * 17 + 19);
            v_consumo := (random() * 299 + 201)::decimal(10,2);
        END IF;

        INSERT INTO usuarios (
            id, nombre, apellido, correo, contrasena, celular, fecha_nacimiento, dni, tipo_documento, genero, rol,
            puntos, es_socio, tiene_discapacidad, fecha_registro, activo, nivel_id, 
            uso_beneficios_mensual, mes_ultimo_beneficio, yearly_visits, consumo_anual_dulceria, 
            fecha_inicio_periodo, fecha_ultima_visita
        ) VALUES (
            v_usuario_id, v_nombre, v_apellido, v_correo, 
            '$2a$10$bf7dnc1eILazvmrzHtlsyujgJHVmP9lzx9ftl5EAuZ1oWD0/VSW1.', 
            v_celular, v_fecha_nacimiento, v_dni, 'DNI', v_genero, 'CLIENT',
            v_puntos, true, false, v_fecha_registro, true, v_nivel_id,
            '{}'::jsonb, v_mes_beneficio, v_yearly_visits, v_consumo,
            v_fecha_inicio_periodo, v_fecha_ultima_visita
        ) ON CONFLICT (correo) DO NOTHING;

        -- Guardar el usuario real insertado (o evadido) para usar sus boletas. 
        INSERT INTO temp_usuarios (id, nivel) VALUES (v_usuario_id, v_nivel_texto);
    END LOOP;

    -- ==========================================
    -- 2. Boletas y Detalles (2 a 4 por usuario)
    -- ==========================================
    FOR v_usuario_id, v_nivel_texto IN SELECT id, nivel FROM temp_usuarios LOOP
        v_num_boletas := floor(random() * 3 + 2); 

        FOR b IN 1..v_num_boletas LOOP
            v_boleta_id := gen_random_uuid();
            -- Si no hay funciones activas, saltar (prevención de array out of bounds)
            IF array_length(v_funciones_ids, 1) IS NULL THEN
                CONTINUE;
            END IF;

            v_funcion_id := v_funciones_ids[floor(random() * array_length(v_funciones_ids, 1) + 1)];
            v_fecha_compra := NOW() - (floor(random() * 90) || ' days')::interval;
            v_monto_total := 0;

            INSERT INTO boletas (id, codigo_unico, funcion_id, usuario_id, cliente_temporal_id, monto_total, estado, fecha_compra)
            VALUES (v_boleta_id, gen_random_uuid(), v_funcion_id, v_usuario_id, NULL, 0, 'VALIDA', v_fecha_compra)
            ON CONFLICT (id) DO NOTHING;

            INSERT INTO temp_boletas (id, usuario_id, nivel, funcion_id) VALUES (v_boleta_id, v_usuario_id, v_nivel_texto, v_funcion_id);

            -- ==========================================
            -- 3. Tickets (1 o 2 por boleta)
            -- ==========================================
            v_num_tickets := floor(random() * 2 + 1);
            SELECT sala_id INTO v_sala_id FROM funciones WHERE id = v_funcion_id;

            FOR t IN 1..v_num_tickets LOOP
                SELECT id INTO v_asiento_id FROM asientos WHERE sala_id = v_sala_id ORDER BY random() LIMIT 1;
                
                -- Evitar insertar tickets si no hay asientos disponibles
                IF v_asiento_id IS NOT NULL THEN
                    IF v_nivel_texto = 'AZUL' THEN
                        v_precio_ticket := (random() * 6 + 12)::decimal(10,2);
                        v_beneficio_id := CASE WHEN random() > 0.3 THEN floor(random() * 3 + 1) ELSE NULL END;
                    ELSIF v_nivel_texto = 'DORADO' THEN
                        v_precio_ticket := (random() * 6 + 10)::decimal(10,2);
                        v_beneficio_id := CASE WHEN random() > 0.3 THEN floor(random() * 4 + 4) ELSE NULL END;
                    ELSE
                        v_precio_ticket := (random() * 6 + 8)::decimal(10,2);
                        v_beneficio_id := CASE WHEN random() > 0.3 THEN floor(random() * 5 + 8) ELSE NULL END;
                    END IF;

                    INSERT INTO boleta_asiento (boleta_id, asiento_id, tipo_entrada, precio_pagado, beneficio_id)
                    VALUES (v_boleta_id, v_asiento_id, 'NORMAL', v_precio_ticket, v_beneficio_id)
                    ON CONFLICT (boleta_id, asiento_id) DO NOTHING;

                    v_monto_total := v_monto_total + v_precio_ticket;
                END IF;
            END LOOP;

            -- ==========================================
            -- 4. Dulcería (70% de probabilidad)
            -- ==========================================
            IF random() > 0.3 THEN
                v_num_dulces := floor(random() * 3 + 1); 
                FOR d IN 1..v_num_dulces LOOP
                    IF v_nivel_texto = 'AZUL' THEN
                        v_producto_id := floor(random() * 7 + 1);
                    ELSIF v_nivel_texto = 'DORADO' THEN
                        v_producto_id := floor(random() * 11 + 1);
                    ELSE
                        v_producto_id := floor(random() * 15 + 1);
                    END IF;

                    v_cantidad := floor(random() * 3 + 1);
                    SELECT precio INTO v_precio_unitario FROM productos WHERE id = v_producto_id;
                    
                    IF v_precio_unitario IS NOT NULL THEN
                        v_precio_total := v_cantidad * v_precio_unitario;
                        INSERT INTO boleta_dulceria (boleta_id, producto_id, cantidad, precio_unitario, precio_total)
                        VALUES (v_boleta_id, v_producto_id, v_cantidad, v_precio_unitario, v_precio_total);

                        v_monto_total := v_monto_total + v_precio_total;
                    END IF;
                END LOOP;
            END IF;

            -- Actualizar monto total boleta
            UPDATE boletas SET monto_total = v_monto_total WHERE id = v_boleta_id;

        END LOOP;
    END LOOP;

    -- Ajustar secuencias al finalizar
    PERFORM setval('boleta_asiento_id_seq', COALESCE((SELECT MAX(id) FROM boleta_asiento), 1));
    PERFORM setval('boleta_dulceria_id_seq', COALESCE((SELECT MAX(id) FROM boleta_dulceria), 1));

END $$;
