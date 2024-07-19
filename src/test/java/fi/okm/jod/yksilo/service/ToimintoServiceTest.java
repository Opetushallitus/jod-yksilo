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
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.service.profiili.PatevyysService;
import fi.okm.jod.yksilo.service.profiili.ToimintoService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/osaaminen.sql")
@Import({ToimintoService.class, PatevyysService.class, YksilonOsaaminenService.class})
class ToimintoServiceTest extends AbstractServiceTest {

  @Autowired ToimintoService service;

  @Test
  void shouldAddToiminto() {
    assertDoesNotThrow(
        () -> {
          var id = service.add(user, new ToimintoDto(null, ls(Kieli.FI, "nimi"), null));
          entityManager.flush();

          var updatedNimi = ls(Kieli.SV, "namn");
          service.update(user, new ToimintoDto(id, updatedNimi, null));

          simulateCommit();

          var result = service.findAll(user);
          assertEquals(1, result.size());
          assertEquals(updatedNimi, result.getFirst().nimi());
        });
  }

  @Test
  void shouldFindToimintoById() {
    assertDoesNotThrow(
        () -> {
          var id = service.add(user, new ToimintoDto(null, ls(Kieli.FI, "nimi"), null));
          entityManager.flush();
          var result = service.find(user, id);
          assertNotNull(result);
          assertEquals(id, result.id());
          assertEquals(ls(Kieli.FI, "nimi"), result.nimi());
        });
  }

  @Test
  void shouldUpdateToiminto() {
    assertDoesNotThrow(
        () -> {
          var id =
              service.add(
                  user,
                  new ToimintoDto(
                      null,
                      ls(Kieli.FI, "nimi"),
                      Set.of(
                          new PatevyysDto(
                              null,
                              ls(Kieli.FI, "nimi"),
                              LocalDate.now(),
                              LocalDate.now(),
                              Set.of()))));

          simulateCommit();

          var updatedNimi = ls(Kieli.SV, "namn");
          service.update(user, new ToimintoDto(id, updatedNimi, Set.of()));

          simulateCommit();

          var result = service.find(user, id);
          assertNotNull(result);
          assertEquals(updatedNimi, result.nimi());
          assertEquals(0, result.patevyydet().size());
        });
  }

  @Test
  void shouldNotUpdateUnrelatedPatevyys() {
    var id =
        service.add(
            user,
            new ToimintoDto(
                null,
                ls(Kieli.FI, "nimi"),
                Set.of(
                    new PatevyysDto(
                        null, ls(Kieli.FI, "nimi"), LocalDate.now(), LocalDate.now(), Set.of()))));
    var id2 =
        service.add(
            user,
            new ToimintoDto(
                null,
                ls(Kieli.FI, "nimi"),
                Set.of(
                    new PatevyysDto(
                        null, ls(Kieli.FI, "nimi"), LocalDate.now(), LocalDate.now(), Set.of()))));

    simulateCommit();

    var dto2 = service.find(user, id2);

    assertThrows(
        ServiceException.class,
        () -> {
          service.update(user, new ToimintoDto(id, dto2.nimi(), dto2.patevyydet()));
          simulateCommit();
        });
  }

  @Test
  void shouldDeleteToiminto() {
    assertThrows(
        NotFoundException.class,
        () -> {
          var id =
              service.add(
                  user,
                  new ToimintoDto(
                      null,
                      ls(Kieli.FI, "nimi"),
                      Set.of(
                          new PatevyysDto(
                              null,
                              ls(Kieli.FI, "nimi"),
                              LocalDate.now(),
                              LocalDate.now(),
                              Set.of()))));
          simulateCommit();

          service.delete(user, Set.of(id));
          simulateCommit();

          var result = service.find(user, id);
          assertNull(result);
        });
  }
}
