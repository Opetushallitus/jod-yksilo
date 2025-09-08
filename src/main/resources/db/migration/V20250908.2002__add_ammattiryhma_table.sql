CREATE TABLE yksilo.ammattiryhma
(
  id                  BIGINT                 NOT NULL PRIMARY KEY ,
  esco_uri            CHARACTER VARYING(255) NOT NULL,
  luotu    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  muokattu TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  mediaani_palkka     INT,
  ylin_desiili_palkka INT,
  alin_desiili_palkka INT
);

CREATE UNIQUE INDEX ar_esco_uri ON ammattiryhma (esco_uri);


