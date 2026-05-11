CREATE TABLE cv_tehtava
(
  id        UUID        NOT NULL PRIMARY KEY,
  yksilo_id UUID        NOT NULL REFERENCES yksilo (id) ON DELETE CASCADE,
  kieli     VARCHAR(2)  NOT NULL,
  tila      VARCHAR(20) NOT NULL DEFAULT 'ODOTTAA' CHECK ( tila IN ('ODOTTAA', 'EPAONNISTUNUT', 'VALMIS') ),
  tulos     JSONB,
  luotu     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  muokattu  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ON cv_tehtava (yksilo_id);

-- Enforces at most one pending task per user
CREATE UNIQUE INDEX idx_cv_tehtava_odottaa
  ON cv_tehtava (yksilo_id)
  WHERE tila = 'ODOTTAA';
