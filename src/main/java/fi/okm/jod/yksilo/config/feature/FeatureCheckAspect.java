/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.feature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
class FeatureCheckAspect {

  private final FeatureConfiguration features;

  @Before("@within(feature)")
  void within(FeatureRequired feature) {
    checkFeatureEnabled(feature.value());
  }

  @Before("@annotation(feature)")
  void annotation(FeatureRequired feature) {
    checkFeatureEnabled(feature.value());
  }

  private void checkFeatureEnabled(Feature feature) {
    if (!features.isFeatureEnabled(feature)) {
      throw new FeatureDisabledException("Feature " + feature + " is disabled");
    }
  }
}
