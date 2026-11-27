-- OPHJOD-3787: ESCO descriptions are merged into the concept rows.
-- The import procedures depend on the description tables; they are recreated by R__400_esco_functions.sql.
DROP PROCEDURE esco_data.import_ammatti();
DROP PROCEDURE esco_data.import_osaaminen();
DROP TABLE esco_data.occupation_descriptions;
DROP TABLE esco_data.skill_descriptions;
