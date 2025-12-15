/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.tmt;

/** Constants defined by TMT API. */
record TmtApiConstants() {

  // https://tyomarkkinatori.fi/api/codes/v1/kopa/TY%C3%96NHAKUPROFIILI_KOULUTUS_TILA/koodit
  static final String KOULUTUS_TILA_ALKAMASSA_TAI_JATKUU = "1";
  static final String KOUTULUS_TILA_PAATTYNYT = "2";
  static final String KOULUTUS_TILA_KESKEYTYNYT = "3";

  // Profile item limits, see OpenAPI definition
  static final int PROFILE_ITEM_LIMIT = 20;
  static final int SKILL_LIMIT = 120;
}
