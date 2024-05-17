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

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import fi.okm.jod.yksilo.service.profiili.ToimenkuvaService;
import fi.okm.jod.yksilo.service.profiili.TyopaikkaService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/osaaminen.sql")
@Import({TyopaikkaService.class, ToimenkuvaService.class, YksilonOsaaminenService.class})
class TyopaikkaServiceTest extends AbstractServiceTest {

  @Autowired TyopaikkaService service;

  @Test
  void shouldAddTyopaikka() {
    assertDoesNotThrow(
        () -> {
          var id = service.add(user, new TyopaikkaDto(null, ls(Kieli.FI, "nimi"), null));
          entityManager.flush();

          var updatedNimi = ls(Kieli.SV, "namn");
          service.update(user, new TyopaikkaDto(id, updatedNimi, null));
          entityManager.flush();
          entityManager.clear();

          var result = service.findAll(user);
          assertEquals(1, result.size());
          assertEquals(updatedNimi, result.getFirst().nimi());
        });
  }
}
