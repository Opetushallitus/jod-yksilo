-- Cleanup always whole database between sets.
-- There should be no data that is common between all tests
TRUNCATE osaaminen CASCADE;
TRUNCATE ammatti CASCADE;
TRUNCATE tyomahdollisuus CASCADE;
TRUNCATE koulutusmahdollisuus CASCADE;
TRUNCATE yksilon_suosikki CASCADE;
TRUNCATE yksilon_osaaminen CASCADE;
TRUNCATE yksilo_ammatti_kiinnostukset CASCADE;
TRUNCATE yksilo_osaamis_kiinnostukset CASCADE;
TRUNCATE yksilo CASCADE;
TRUNCATE paamaara CASCADE;
