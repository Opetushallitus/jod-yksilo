CREATE SEQUENCE IF NOT EXISTS osaaminen_seq;
SELECT setval('osaaminen_seq', (SELECT COALESCE(MAX(id), 1) FROM osaaminen));

INSERT INTO public.osaaminen (id, uri) SELECT nextval('osaaminen_seq'), "conceptUri"  AS uri FROM esco_data.skills_fi;
INSERT INTO public.osaaminen_kaannos (osaaminen_id, kaannos_key,  kuvaus, nimi) SELECT o.id AS osaaminen_id, 'FI' AS kaannos_key, description AS kuvaus, "preferredLabel"  AS nimi FROM esco_data.skills_fi e JOIN public.osaaminen o ON o.uri = e."conceptUri";
INSERT INTO public.osaaminen_kaannos (osaaminen_id, kaannos_key,  kuvaus, nimi) SELECT o.id AS osaaminen_id, 'SV' AS kaannos_key, description AS kuvaus, "preferredLabel"  AS nimi FROM esco_data.skills_sv e JOIN public.osaaminen o ON o.uri = e."conceptUri";
INSERT INTO public.osaaminen_kaannos (osaaminen_id, kaannos_key,  kuvaus, nimi) SELECT o.id AS osaaminen_id, 'EN' AS kaannos_key, description AS kuvaus, "preferredLabel"  AS nimi FROM esco_data.skills_en e JOIN public.osaaminen o ON o.uri = e."conceptUri";
