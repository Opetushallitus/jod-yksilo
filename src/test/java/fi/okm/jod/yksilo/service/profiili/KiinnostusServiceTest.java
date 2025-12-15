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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({KiinnostusService.class})
class KiinnostusServiceTest extends AbstractServiceTest {
  @Autowired KiinnostusService service;
  @Autowired private YksiloRepository yksilot;

  @Test
  void addKiinnostukset200() {
    Instant afterCreationBeforeUpdate = Instant.now();
    final Set<URI> kiinnostukset =
        Set.of(
            URI.create("urn:ammatti:1"),
            URI.create("urn:ammatti:2"),
            URI.create("urn:ammatti:3"),
            URI.create("urn:osaaminen:1"),
            URI.create("urn:osaaminen:2"),
            URI.create("urn:osaaminen:3"),
            URI.create("urn:osaaminen:4"),
            URI.create("urn:osaaminen:5"));

    this.service.updateOsaamiset(user, kiinnostukset);
    simulateCommit();

    final Yksilo yksilo = this.yksilot.getReferenceById(user.getId());
    final Set<String> ammattiKiinnostukset = yksilot.findAmmattiKiinnostukset(yksilo);
    final Set<String> osaamisKiinnostukset = yksilot.findOsaamisKiinnostukset(yksilo);
    assertEquals(ammattiKiinnostukset, Set.of("urn:ammatti:1", "urn:ammatti:2", "urn:ammatti:3"));
    assertEquals(
        osaamisKiinnostukset,
        Set.of(
            "urn:osaaminen:1",
            "urn:osaaminen:2",
            "urn:osaaminen:3",
            "urn:osaaminen:4",
            "urn:osaaminen:5"));
    Instant yksiloMuokattu = yksilo.getMuokattu();
    assertTrue(yksiloMuokattu.isAfter(afterCreationBeforeUpdate));
  }

  @Test
  void testUpdateOsaamisKiinnostukset() {
    final Set<URI> kiinnostukset =
        Set.of(URI.create("urn:osaaminen:1"), URI.create("urn:osaaminen:2"));
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
