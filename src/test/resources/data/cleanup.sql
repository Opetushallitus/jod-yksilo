-- Cleanup always whole database between sets.
-- There should be no data that is common between all tests
DELETE
FROM osaaminen
WHERE uri NOT IN
      ('urn:osaaminen1', 'urn:osaaminen2', 'urn:osaaminen3', 'urn:osaaminen4', 'urn:osaaminen5',
       'urn:osaaminen6', 'urn:osaaminen7');
TRUNCATE ammatti CASCADE;
TRUNCATE tyomahdollisuus CASCADE;
TRUNCATE koulutusmahdollisuus CASCADE;
TRUNCATE yksilon_suosikki CASCADE;
TRUNCATE yksilon_osaaminen CASCADE;
TRUNCATE yksilo_ammatti_kiinnostukset CASCADE;
TRUNCATE yksilo_osaamis_kiinnostukset CASCADE;
TRUNCATE yksilo CASCADE;
TRUNCATE paamaara CASCADE;
