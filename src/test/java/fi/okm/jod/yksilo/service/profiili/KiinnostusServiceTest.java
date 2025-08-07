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
import org.springframework.test.context.jdbc.Sql;

@Import({KiinnostusService.class})
class KiinnostusServiceTest extends AbstractServiceTest {
  @Autowired KiinnostusService service;
  @Autowired private YksiloRepository yksilot;

  @Sql(
      scripts = {"/data/ammatti.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Test
  void addKiinnostukset200() {
    Instant afterCreationBeforeUpdate = Instant.now();
    final Set<URI> kiinnostukset =
        Set.of(
            URI.create("urn:ammatti1"),
            URI.create("urn:ammatti2"),
            URI.create("urn:ammatti3"),
            URI.create("urn:osaaminen1"),
            URI.create("urn:osaaminen2"),
            URI.create("urn:osaaminen3"),
            URI.create("urn:osaaminen4"),
            URI.create("urn:osaaminen5"));

    this.service.updateOsaamiset(user, kiinnostukset);
    simulateCommit();

    final Yksilo yksilo = this.yksilot.getReferenceById(user.getId());
    final Set<String> ammattiKiinnostukset = yksilot.findAmmattiKiinnostukset(yksilo);
    final Set<String> osaamisKiinnostukset = yksilot.findOsaamisKiinnostukset(yksilo);
    assertEquals(ammattiKiinnostukset, Set.of("urn:ammatti1", "urn:ammatti2", "urn:ammatti3"));
    assertEquals(
        osaamisKiinnostukset,
        Set.of(
            "urn:osaaminen1",
            "urn:osaaminen2",
            "urn:osaaminen3",
            "urn:osaaminen4",
            "urn:osaaminen5"));
    Instant yksiloMuokattu = yksilo.getMuokattu();
    assertTrue(yksiloMuokattu.isAfter(afterCreationBeforeUpdate));
  }

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
