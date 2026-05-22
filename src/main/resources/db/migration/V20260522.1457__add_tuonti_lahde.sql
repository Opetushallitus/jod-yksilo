ALTER TABLE koulutus_kokonaisuus
  ADD COLUMN tuonti_lahde VARCHAR(20);

ALTER TABLE koulutus_kokonaisuus
  ADD CONSTRAINT ck_koulutus_kokonaisuus_tuonti_lahde
  CHECK (tuonti_lahde IN ('CV_TUONTI', 'KOSKI_TUONTI', 'TMT_TUONTI'));

ALTER TABLE tyopaikka
  ADD COLUMN tuonti_lahde VARCHAR(20);

ALTER TABLE tyopaikka
  ADD CONSTRAINT ck_tyopaikka_tuonti_lahde
    CHECK (tuonti_lahde IN ('CV_TUONTI', 'KOSKI_TUONTI', 'TMT_TUONTI'));

ALTER TABLE toiminto
  ADD COLUMN tuonti_lahde VARCHAR(20);

ALTER TABLE toiminto
  ADD CONSTRAINT ck_toiminto_tuonti_lahde
    CHECK (tuonti_lahde IN ('CV_TUONTI', 'KOSKI_TUONTI', 'TMT_TUONTI'));

