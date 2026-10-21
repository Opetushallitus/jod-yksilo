-- OPHJOD-1455: Identify the Koski opiskeluoikeus an imported koulutus_kokonaisuus originates from

CREATE TABLE koulutus_kokonaisuus_koski_tunniste
(
  koulutus_kokonaisuus_id uuid PRIMARY KEY REFERENCES koulutus_kokonaisuus (id) ON DELETE CASCADE,
  oid                     text,
  lahdejarjestelma        text,
  lahdejarjestelma_id     text,
  toimija_oid             text,
  -- The fields are compared as a whole, so only the canonical forms are allowed: either the oid
  -- alone, or the id in the source system with the optional lahdejarjestelma and toimija_oid.
  -- All values are non-empty.
  CONSTRAINT ck_koulutus_kokonaisuus_koski_tunniste CHECK (
    oid <> '' AND lahdejarjestelma <> '' AND lahdejarjestelma_id <> '' AND toimija_oid <> ''
      AND (
      (oid IS NOT NULL AND num_nonnulls(lahdejarjestelma, lahdejarjestelma_id, toimija_oid) = 0)
        OR (oid IS NULL AND lahdejarjestelma_id IS NOT NULL)
      )
    )
);
