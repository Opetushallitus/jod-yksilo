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

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusUpdateDto;
import fi.okm.jod.yksilo.service.profiili.KoulutusKokonaisuusService;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({KoulutusKokonaisuusService.class, KoulutusService.class, YksilonOsaaminenService.class})
class KoulutusKokonaisuusServiceTest extends AbstractServiceTest {

  @Autowired KoulutusKokonaisuusService service;

  @Test
  void shouldAddKoulutusKokonaisuus() {
    assertDoesNotThrow(
        () -> {
          var id =
              service.add(
                  user,
                  new KoulutusKokonaisuusDto(null, ls(Kieli.FI, "nimi"), Collections.emptySet()));
          entityManager.flush();

          var updatedNimi = ls(Kieli.SV, "namn");
          service.update(user, new KoulutusKokonaisuusUpdateDto(id, updatedNimi));

          simulateCommit();

          var result = service.findAll(user);
          assertEquals(1, result.size());
          assertEquals(updatedNimi, result.getFirst().nimi());
        });
  }

  @Test
  void shouldRemoveKoulutusKokonaisuus() {
    assertDoesNotThrow(
        () -> {
          var id =
              service.add(
                  user,
                  new KoulutusKokonaisuusDto(
                      null,
                      ls(Kieli.FI, "nimi"),
                      Set.of(
                          new KoulutusDto[] {
                            new KoulutusDto(
                                null,
                                ls(Kieli.FI, "nimi"),
                                ls(Kieli.FI, "kuvaus"),
                                LocalDate.now(),
                                null,
                                Set.of(URI.create("urn:osaaminen:1")),
                                null,
                                null,
                                null)
                          })));

          simulateCommit();

          service.delete(user, id);
          simulateCommit();
        });
  }
}
