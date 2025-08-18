ALTER TABLE yksilo
  ADD aidinkieli                         VARCHAR(2),
  ADD valittu_kieli                      VARCHAR(2),
  ADD kotikunta                          VARCHAR(3),
  ADD sukupuoli                          VARCHAR(255) CHECK (sukupuoli IN ('MIES', 'NAINEN')),
  ADD syntymavuosi                       INTEGER;

COMMENT ON COLUMN yksilo.aidinkieli IS 'ISO 639-1 set-1';
COMMENT ON COLUMN yksilo.valittu_kieli IS 'ISO 639-1 set-1';
COMMENT ON COLUMN yksilo.kotikunta IS 'https://stat.fi/fi/luokitukset/kunta';
