-- Fix constraint for tipo_entrada in boleta_asiento
ALTER TABLE boleta_asiento DROP CONSTRAINT IF EXISTS boleta_asiento_tipo_entrada_check;
ALTER TABLE boleta_asiento ADD CONSTRAINT boleta_asiento_tipo_entrada_check 
CHECK (tipo_entrada IN ('NORMAL','TERCERA_EDAD','DISCAPACIDAD','NINO','BENEFICIO','VIP'));

-- Fix constraint for ticket_type in ticket_base_prices_v2
ALTER TABLE ticket_base_prices_v2 DROP CONSTRAINT IF EXISTS ticket_base_prices_v2_ticket_type_check;
ALTER TABLE ticket_base_prices_v2 ADD CONSTRAINT ticket_base_prices_v2_ticket_type_check 
CHECK (ticket_type IN ('NORMAL','TERCERA_EDAD','DISCAPACIDAD','NINO','BENEFICIO','VIP'));
