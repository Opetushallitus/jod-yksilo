ALTER TABLE toiminto RENAME TO teema;
ALTER TABLE toiminto_kaannos RENAME TO teema_kaannos;

ALTER TABLE teema_kaannos RENAME COLUMN toiminto_id TO teema_id;
ALTER TABLE patevyys RENAME COLUMN toiminto_id TO teema_id;

ALTER TABLE teema RENAME CONSTRAINT toiminto_pkey TO teema_pkey;
ALTER TABLE teema_kaannos RENAME CONSTRAINT toiminto_kaannos_pkey TO teema_kaannos_pkey;
ALTER TABLE teema_kaannos RENAME CONSTRAINT toiminto_kaannos_kaannos_key_check TO teema_kaannos_kaannos_key_check;

ALTER TABLE jakolinkki_toiminnot RENAME TO jakolinkki_teemat;
ALTER TABLE jakolinkki_teemat RENAME COLUMN toiminto_id TO teema_id;
ALTER TABLE jakolinkki_teemat RENAME CONSTRAINT pk_jakolinkki_toiminto TO pk_jakolinkki_teema;
ALTER TABLE jakolinkki_teemat RENAME CONSTRAINT fk_jakolinkki_toiminto_jakolinkki TO fk_jakolinkki_teema_jakolinkki;
ALTER TABLE jakolinkki_teemat RENAME CONSTRAINT fk_jakolinkki_toiminto_toiminto TO fk_jakolinkki_teema_teema;
ALTER INDEX idx_jakolinkki_toiminto_jakolinkki RENAME TO idx_jakolinkki_teema_jakolinkki;

ALTER TABLE teema RENAME CONSTRAINT ck_toiminto_tuonti_lahde TO ck_teema_tuonti_lahde;
