CREATE TEMPORARY TABLE ammattiryhma_import (
  esco_uri TEXT,
  data     JSONB
);
COPY ammattiryhma_import (esco_uri, data) FROM STDIN CSV HEADER;

INSERT INTO ammattiryhma (esco_uri, data)
SELECT esco_uri, data
FROM ammattiryhma_import
ON CONFLICT (esco_uri) DO UPDATE SET data = EXCLUDED.data, muokattu = now();
