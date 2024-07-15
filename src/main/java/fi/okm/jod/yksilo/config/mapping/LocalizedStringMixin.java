/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.mapping;

import com.fasterxml.jackson.annotation.JsonValue;
import fi.okm.jod.yksilo.domain.Kieli;
import java.util.Map;

// Workaround for SpringDoc ignoring Schema override if the JsonValue annotation
// is in the actual class
interface LocalizedStringMixin {
  @JsonValue
  Map<Kieli, String> asMap();
}
