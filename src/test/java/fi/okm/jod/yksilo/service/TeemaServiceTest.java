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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.TuontiLahde;
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.dto.profiili.TeemaDto;
import fi.okm.jod.yksilo.dto.profiili.TeemaUpdateDto;
import fi.okm.jod.yksilo.service.profiili.PatevyysService;
import fi.okm.jod.yksilo.service.profiili.TeemaService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({TeemaService.class, PatevyysService.class, YksilonOsaaminenService.class})
class TeemaServiceTest extends AbstractServiceTest {

  @Autowired TeemaService service;

  @Test
  void shouldAddTeema() {
    assertDoesNotThrow(
        () -> {
          var id = service.add(user, new TeemaDto(null, ls(Kieli.FI, "nimi"), null, null));
          entityManager.flush();

          var updatedNimi = ls(Kieli.SV, "namn");
          service.update(user, new TeemaUpdateDto(id, updatedNimi));

          simulateCommit();

          var result = service.findAll(user);
          assertEquals(1, result.size());
          assertEquals(updatedNimi, result.getFirst().nimi());
        });
  }

  @Test
  void shouldGetTeemaById() {
    assertDoesNotThrow(
        () -> {
          var id = service.add(user, new TeemaDto(null, ls(Kieli.FI, "nimi"), null, null));
          entityManager.flush();
          var result = service.get(user, id);
          assertNotNull(result);
          assertEquals(id, result.id());
          assertEquals(ls(Kieli.FI, "nimi"), result.nimi());
        });
  }

  @Test
  void shouldDeleteTeema() {
    var id =
        service.add(
            user,
            new TeemaDto(
                null,
                ls(Kieli.FI, "nimi"),
                null,
                Set.of(
                    new PatevyysDto(
                        null,
                        ls(Kieli.FI, "nimi"),
                        ls("Kuvaus"),
                        LocalDate.now(),
                        LocalDate.now(),
                        Set.of()))));
    simulateCommit();

    service.delete(user, id);
    simulateCommit();

    assertThrows(NotFoundException.class, () -> service.get(user, id));
  }

  @Test
  void shouldIgnoreTuontiLahdeFromUserFacingAdd() {
    // tuontiLahde supplied by API clients must be silently ignored
    var id =
        service.add(user, new TeemaDto(null, ls(Kieli.FI, "nimi"), TuontiLahde.TMT_TUONTI, null));

    simulateCommit();

    var result = service.get(user, id);
    assertNull(result.tuontiLahde());
  }

  @Test
  void shouldPersistTuontiLahdeForImport() {
    var id =
        service
            .addFromImport(
                user,
                Set.of(new TeemaDto(null, ls(Kieli.FI, "nimi"), TuontiLahde.TMT_TUONTI, null)))
            .getFirst();

    simulateCommit();

    var result = service.get(user, id);
    assertEquals(TuontiLahde.TMT_TUONTI, result.tuontiLahde());
  }

  @Test
  void shouldClearTuontiLahdeOnTeemaUpdate() {
    var id =
        service
            .addFromImport(
                user, Set.of(new TeemaDto(null, ls(Kieli.FI, "nimi"), TuontiLahde.CV_TUONTI, null)))
            .getFirst();

    service.update(user, new TeemaUpdateDto(id, ls(Kieli.SV, "namn")));
    simulateCommit();

    var result = service.get(user, id);
    assertNull(result.tuontiLahde());
  }

  @Test
  void shouldPreserveTuontiLahdeAfterImport() {
    // tuontiLahde must survive child creation during import (same transaction)
    var id =
        service
            .addFromImport(
                user,
                Set.of(
                    new TeemaDto(
                        null,
                        ls(Kieli.FI, "nimi"),
                        TuontiLahde.TMT_TUONTI,
                        Set.of(
                            new PatevyysDto(
                                null,
                                ls(Kieli.FI, "patevyys"),
                                null,
                                LocalDate.now(),
                                LocalDate.now(),
                                Set.of())))))
            .getFirst();
    simulateCommit();

    var result = service.get(user, id);
    assertEquals(TuontiLahde.TMT_TUONTI, result.tuontiLahde());
  }
}
