DO '
DECLARE
  koulutusMahdollisuusId UUID = ''f47ac10b-58cc-4372-a567-0e02b2c3d479'';
BEGIN
    INSERT INTO koulutusmahdollisuus (id, tyyppi)
    VALUES (koulutusMahdollisuusId, ''TUTKINTO'');
END
'
