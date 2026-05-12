/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

import java.util.UUID;

/** A row from the tunnistus.henkilo table. */
record HenkiloRow(UUID yksiloId, String henkiloId, String etunimi, String sukunimi) {

  /** Extracts the hetu by stripping the "FIN:" prefix from henkiloId. */
  String hetu() {
    return henkiloId.substring("FIN:".length());
  }
}
