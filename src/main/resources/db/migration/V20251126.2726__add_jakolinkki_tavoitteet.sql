
CREATE TABLE yksilo.jakolinkki_tavoitteet (
  jakolinkki_id       uuid NOT NULL,
  tavoite_id        uuid NOT NULL,
  CONSTRAINT pk_jakolinkki_tavoite PRIMARY KEY (jakolinkki_id, tavoite_id),
  CONSTRAINT fk_jakolinkki_tavoite_jakolinkki FOREIGN KEY (jakolinkki_id) REFERENCES yksilo.jakolinkki (id) ON DELETE CASCADE,
  CONSTRAINT fk_jakolinkki_tavoite_tavoite FOREIGN KEY (tavoite_id) REFERENCES yksilo.tavoite (id) ON DELETE CASCADE
);

CREATE INDEX idx_jakolinkki_tavoite_jakolinkki ON yksilo.jakolinkki_tavoitteet USING btree (jakolinkki_id);
