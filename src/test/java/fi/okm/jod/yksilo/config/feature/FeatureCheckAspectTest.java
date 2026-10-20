/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class FeatureCheckAspectTest {

  static class Target {
    @FeatureRequired(Feature.SAMANKALTAISET_MAHDOLLISUUDET)
    public String call() {
      return "ok";
    }
  }

  private static Target proxyWithFlags(Map<Feature, Boolean> flags) {
    var features = new FeatureConfiguration();
    features.setFlags(flags);
    var factory = new AspectJProxyFactory(new Target());
    factory.setProxyTargetClass(true);
    factory.addAspect(new FeatureCheckAspect(features));
    return factory.getProxy();
  }

  @Test
  void shouldAllowCallWhenFeatureIsEnabled() {
    var target = proxyWithFlags(Map.of(Feature.SAMANKALTAISET_MAHDOLLISUUDET, true));

    assertThat(target.call()).isEqualTo("ok");
  }

  @Test
  void shouldRejectCallWhenFeatureIsDisabled() {
    var target = proxyWithFlags(Map.of(Feature.SAMANKALTAISET_MAHDOLLISUUDET, false));

    assertThatThrownBy(target::call).isInstanceOf(FeatureDisabledException.class);
  }

  @Test
  void shouldRejectCallWhenFeatureIsNotConfigured() {
    var target = proxyWithFlags(Map.of());

    assertThatThrownBy(target::call).isInstanceOf(FeatureDisabledException.class);
  }
}
