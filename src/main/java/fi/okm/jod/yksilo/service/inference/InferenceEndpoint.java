/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.inference;

import org.jspecify.annotations.Nullable;

/**
 * Inference endpoint.
 *
 * @param name endpoint name (SageMaker endpoint name or URL when using REST)
 * @param component optional SageMaker inference component name
 */
public record InferenceEndpoint(String name, @Nullable String component) {

  public InferenceEndpoint {
    if (component != null && component.isBlank()) {
      component = null;
    }
  }

  public InferenceEndpoint(String name) {
    this(name, null);
  }

  public static InferenceEndpoint of(String name) {
    return new InferenceEndpoint(name);
  }
}
