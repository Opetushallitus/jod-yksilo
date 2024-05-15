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
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/osaaminen.sql")
@Import(KoulutusService.class)
class KoulutusServiceTest extends AbstractServiceTest {

  @Autowired KoulutusService service;

  @Test
  void shouldAddKoulutus() {
    assertDoesNotThrow(
        () -> {
          var id = service.add(user, new KoulutusDto(ls("nimi"), null, null, null));
          entityManager.flush();

          var updatedNimi = ls(Kieli.SV, "namn");
          service.update(
              user, new KoulutusDto(id, updatedNimi, null, LocalDate.of(2024, 1, 1), null));
          entityManager.flush();
          entityManager.clear();

          var result = service.findAll(user);
          assertEquals(1, result.size());
          assertEquals(updatedNimi, result.getFirst().nimi());
        });
  }
}
