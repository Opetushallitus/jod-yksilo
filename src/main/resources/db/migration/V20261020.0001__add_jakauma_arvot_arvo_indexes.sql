-- Index for finding the mahdollisuudet that share a given jakauma value (e.g. an osaaminen).
-- Used by the similar mahdollisuudet queries. The existing unique constraints only support lookups
-- starting from the jakauma id.

CREATE INDEX tyomahdollisuus_jakauma_arvot_arvo_idx
  ON tyomahdollisuus_jakauma_arvot (arvo, tyomahdollisuus_jakauma_id);

CREATE INDEX koulutusmahdollisuus_jakauma_arvot_arvo_idx
  ON koulutusmahdollisuus_jakauma_arvot (arvo, koulutusmahdollisuus_jakauma_id);
