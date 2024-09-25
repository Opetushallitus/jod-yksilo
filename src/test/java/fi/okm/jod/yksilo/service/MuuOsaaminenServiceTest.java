/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.okm.jod.yksilo.domain.MuuOsaaminen;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.service.profiili.MuuOsaaminenService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/osaaminen.sql")
@Import({MuuOsaaminenService.class, YksilonOsaaminenService.class})
class MuuOsaaminenServiceTest extends AbstractServiceTest {

  @Autowired private MuuOsaaminenService service;

  @BeforeEach
  public void setUp() {
    final Yksilo yksilo = entityManager.find(Yksilo.class, user.getId());
    var muuOsaaminen = new MuuOsaaminen(yksilo, Collections.emptySet());

    entityManager.persist(
        new YksilonOsaaminen(muuOsaaminen, entityManager.find(Osaaminen.class, 1L)));
    simulateCommit();
  }

  @Test
  void shouldFindYksilonMuuOsaaminen() {
    var result = service.findAll(user);
    assertEquals(1, result.size());
  }

  @Test
  void shouldAddNewByUpdateYksilonMuuOsaaminen() {
    service.update(user, Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen3")));
    var result = service.findAll(user);
    assertEquals(2, result.size());
    assertTrue(result.contains(URI.create("urn:osaaminen1")), "result contains osaaminen1");
    assertTrue(result.contains(URI.create("urn:osaaminen3")), "result contains osaaminen3");
    assertFalse(
        result.contains(URI.create("urn:osaaminen2")), "result does not contain osaaminen2");
  }

  @Test
  void shouldDeleteNewByUpdateYksilonMuuOsaaminen() {
    service.update(user, Collections.emptySet());
    var result = service.findAll(user);
    assertEquals(0, result.size());
  }
}
