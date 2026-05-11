/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import java.util.UUID;

/** Storage backend for uploaded CV PDFs. */
public interface CvStorage {
  /** Uploads a CV PDF and returns the storage key. */
  String upload(UUID taskId, UUID userId, byte[] pdf);
}
