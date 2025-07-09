/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.assertj.core.api.Assertions.assertThat;

import fi.okm.jod.yksilo.service.AbstractServiceTest;
import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({KiinnostusService.class})
class KiinnostusServiceTest extends AbstractServiceTest {
  @Autowired KiinnostusService service;

  @Test
  void testUpdateOsaamisKiinnostukset() {
    final Set<URI> kiinnostukset =
        Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"));
    service.updateOsaamiset(user, kiinnostukset);
    simulateCommit();
    assertThat(service.getOsaamiset(user)).containsAll(kiinnostukset);
  }

  @Test
  void testUpdateOsaamisKiinnostuksetVapaateksti() {
    var vapaateksti = ls("Testi vapaateksti");
    service.updateVapaateksti(user, vapaateksti);
    simulateCommit();
    assertThat(service.getVapaateksti(user)).isEqualTo(vapaateksti);
  }
}
