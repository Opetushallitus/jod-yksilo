/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Response of the education extraction service: the skills of each education, by its id. */
record KoskiExtractionResponse(
    UUID taskId, UUID userId, String status, Map<UUID, List<URI>> osaamiset) {

  static final String SUCCESS = "SUCCESS";

  static KoskiExtractionResponse failed(UUID taskId, UUID userId) {
    return new KoskiExtractionResponse(taskId, userId, "FAILED", null);
  }
}
