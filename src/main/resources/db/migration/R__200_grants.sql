GRANT USAGE
  ON SCHEMA esco_data, tyomahdollisuus_data, koulutusmahdollisuus_data
  TO dataloader;

GRANT SELECT, INSERT, UPDATE, DELETE
  ON ammattiryhma
  TO dataloader;

GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE
  ON ALL TABLES IN SCHEMA esco_data, tyomahdollisuus_data, koulutusmahdollisuus_data
  TO dataloader;

GRANT EXECUTE
  ON ALL FUNCTIONS IN SCHEMA esco_data, tyomahdollisuus_data, koulutusmahdollisuus_data
  TO dataloader;
