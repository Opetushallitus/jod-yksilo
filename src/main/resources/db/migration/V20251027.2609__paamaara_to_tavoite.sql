ALTER TABLE paamaara RENAME TO tavoite;
ALTER TABLE paamaara_kaannos RENAME TO tavoite_kaannos;

ALTER TABLE tavoite_kaannos DROP CONSTRAINT fkpscq7acrnhqruqeitw7puana1;
ALTER TABLE tavoite_kaannos RENAME COLUMN paamaara_id TO tavoite_id;
ALTER TABLE tavoite_kaannos
  ADD CONSTRAINT fk_tk_tavoite_id
    FOREIGN KEY (tavoite_id)
      REFERENCES tavoite (id);

ALTER TABLE polun_suunnitelma DROP CONSTRAINT fk7td9koaembgy6o5su5xgk0ch4;
ALTER TABLE polun_suunnitelma RENAME COLUMN paamaara_id TO tavoite_id;
ALTER TABLE polun_suunnitelma
  ADD CONSTRAINT fk_ps_tavoite_id
    FOREIGN KEY (tavoite_id)
      REFERENCES tavoite (id);
ALTER TABLE tavoite DROP COLUMN IF EXISTS tyyppi;
