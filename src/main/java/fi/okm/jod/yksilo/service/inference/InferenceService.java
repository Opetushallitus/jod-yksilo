/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.inference;

import java.util.UUID;

public interface InferenceService<T, R> {

  R infer(String endpoint, T payload, Class<R> responseType);

  InferenceSession<R> infer(String endpoint, UUID sessionId, T payload, Class<R> responseType);

  record InferenceSession<R>(R data, UUID sessionId) {}
}
