/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.junit.jupiter.api.Assertions.*;

import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/osaaminen.sql")
@Import(YksilonOsaaminenService.class)
class YksilonOsaaminenServiceTest extends AbstractServiceTest {

  @Autowired YksilonOsaaminenService service;

  private UUID toimenkuvaId;

  @BeforeEach
  public void setUp() {
    final Yksilo yksilo = entityManager.find(Yksilo.class, user.getId());
    var tyopaikka = new Tyopaikka(yksilo, ls("Testi"));
    tyopaikka = entityManager.persist(tyopaikka);

    var toimenkuva = new Toimenkuva(tyopaikka);
    toimenkuva.setNimi(ls("Toimenkuva"));
    toimenkuva = entityManager.persist(toimenkuva);
    this.toimenkuvaId = toimenkuva.getId();

    entityManager.persist(
        new YksilonOsaaminen(toimenkuva, entityManager.find(Osaaminen.class, 1L)));
    simulateCommit();
  }

  @Test
  void shouldFindYksilonOsaaminen() {
    var result = service.findAll(user, OsaamisenLahdeTyyppi.TOIMENKUVA, this.toimenkuvaId);
    assertEquals(1, result.size());
  }
}
