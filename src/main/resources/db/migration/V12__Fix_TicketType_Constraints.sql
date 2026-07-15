-- Fix constraint for tipo_entrada in boletas_entradas
ALTER TABLE boletas_entradas DROP CONSTRAINT IF EXISTS boletas_entradas_tipo_entrada_check;
ALTER TABLE boletas_entradas ADD CONSTRAINT boletas_entradas_tipo_entrada_check 
CHECK (tipo_entrada IN ('NORMAL','TERCERA_EDAD','DISCAPACIDAD','NINO','BENEFICIO','VIP'));

-- Fix constraint for ticket_type in ticket_base_prices_v2
ALTER TABLE ticket_base_prices_v2 DROP CONSTRAINT IF EXISTS ticket_base_prices_v2_ticket_type_check;
ALTER TABLE ticket_base_prices_v2 ADD CONSTRAINT ticket_base_prices_v2_ticket_type_check 
CHECK (ticket_type IN ('NORMAL','TERCERA_EDAD','DISCAPACIDAD','NINO','BENEFICIO','VIP'));

-- Migrate orphaned pending benefits
UPDATE beneficios_pendientes 
SET tipo_beneficio = 'ENTRADA_GRATIS_CUMPLEANOS' 
WHERE tipo_beneficio LIKE 'ENTRADA_GRATIS_CUMPLEA%';
