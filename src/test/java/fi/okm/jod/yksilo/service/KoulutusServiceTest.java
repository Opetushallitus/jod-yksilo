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
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/osaaminen.sql")
@Import({KoulutusService.class, YksilonOsaaminenService.class})
class KoulutusServiceTest extends AbstractServiceTest {

  @Autowired KoulutusService service;
  private UUID kokonaisuusId;

  @BeforeEach
  public void setUp() {
    var kokonaisuus =
        new KoulutusKokonaisuus(
            entityManager.find(Yksilo.class, user.getId()),
            new LocalizedString(Map.of(Kieli.FI, "Testi")));
    this.kokonaisuusId = entityManager.persist(kokonaisuus).getId();
    entityManager.flush();
  }

  @Test
  void shouldAddKoulutus() {
    assertDoesNotThrow(
        () -> {
          service.add(
              user,
              kokonaisuusId,
              new KoulutusDto(
                  null,
                  ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                  null,
                  LocalDate.of(2021, 1, 1),
                  LocalDate.of(2021, 12, 31),
                  Set.of(URI.create("urn:osaaminen1"))));
          entityManager.flush();
          entityManager.clear();

          var result = service.findAll(user, kokonaisuusId);
          assertEquals(1, result.size());
        });
  }

  @Test
  void shouldUpdateKoulutus() {
    assertDoesNotThrow(
        () -> {
          var id =
              service.add(
                  user,
                  kokonaisuusId,
                  new KoulutusDto(
                      null,
                      ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                      null,
                      LocalDate.of(2021, 1, 1),
                      null,
                      Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"))));
          entityManager.flush();
          entityManager.clear();

          var updated =
              Set.of(
                  URI.create("urn:osaaminen2"),
                  URI.create("urn:osaaminen6"),
                  URI.create("urn:osaaminen5"),
                  URI.create("urn:osaaminen4"));
          service.update(
              user,
              kokonaisuusId,
              new KoulutusDto(
                  id,
                  ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                  null,
                  LocalDate.of(2021, 1, 1),
                  LocalDate.of(2021, 12, 31),
                  updated));
          entityManager.flush();
          entityManager.clear();

          var result = service.get(user, kokonaisuusId, id);
          assertEquals(updated, result.osaamiset());
        });
  }

  @Test
  void shouldDeleteEmptyKoulutusKokonaisuus() {
    var id =
        service.add(
            user,
            kokonaisuusId,
            new KoulutusDto(
                null,
                ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                null,
                LocalDate.of(2021, 1, 1),
                null,
                Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"))));
    simulateCommit();
    service.delete(user, kokonaisuusId, id);
    simulateCommit();
    assertNull(entityManager.find(KoulutusKokonaisuus.class, kokonaisuusId));
  }

  @Test
  void shouldKeepNotEmptyKoulutusKokonaisuus() {
    service.add(
        user,
        kokonaisuusId,
        new KoulutusDto(
            null,
            ls(Kieli.FI, "nimi1", Kieli.SV, "namn"),
            null,
            LocalDate.of(2021, 1, 1),
            null,
            Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"))));

    var id =
        service.add(
            user,
            kokonaisuusId,
            new KoulutusDto(
                null,
                ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                null,
                LocalDate.of(2021, 1, 1),
                null,
                Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"))));
    simulateCommit();
    service.delete(user, kokonaisuusId, id);
    simulateCommit();
    assertNotNull(entityManager.find(KoulutusKokonaisuus.class, kokonaisuusId));
  }
}
