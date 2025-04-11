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
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

@Sql(value = "/data/osaaminen.sql", executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
@Import({KoulutusService.class, YksilonOsaaminenService.class})
class KoulutusServiceTest extends AbstractServiceTest {

  @Autowired KoulutusService service;
  private UUID kokonaisuusId;

  @BeforeEach
  void setUp() {
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
                  Set.of(URI.create("urn:osaaminen1")),
                  null,
                  null,
                  null));
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
                      Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2")),
                      null,
                      null,
                      null));
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
                  updated,
                  null,
                  null,
                  null));
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
                Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2")),
                null,
                null,
                null));
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
            Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2")),
            null,
            null,
            null));

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
                Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2")),
                null,
                null,
                null));
    simulateCommit();
    service.delete(user, kokonaisuusId, id);
    simulateCommit();
    assertNotNull(entityManager.find(KoulutusKokonaisuus.class, kokonaisuusId));
  }

  @Test
  void shouldCompleteOsaamisetTunnistus() {
    var koulutus1 = new Koulutus(entityManager.find(KoulutusKokonaisuus.class, kokonaisuusId));
    koulutus1.setOsaamisenTunnistusStatus(OsaamisenTunnistusStatus.WAIT);

    var koulutusKokonaisuus2 =
        new KoulutusKokonaisuus(
            entityManager.find(Yksilo.class, user.getId()),
            new LocalizedString(Map.of(Kieli.FI, "Testi 2")));
    entityManager.persist(koulutusKokonaisuus2);
    var koulutus2 = new Koulutus(koulutusKokonaisuus2);
    koulutus2.setOsaamisenTunnistusStatus(OsaamisenTunnistusStatus.WAIT);

    entityManager.persist(koulutus1);
    entityManager.persist(koulutus2);
    simulateCommit();

    service.completeOsaamisetTunnistus(
        koulutus1, OsaamisenTunnistusStatus.DONE, Set.of(URI.create("urn:osaaminen1")));
    service.completeOsaamisetTunnistus(koulutus2, OsaamisenTunnistusStatus.FAIL, null);
    simulateCommit();

    var updatedKoulutus1 = entityManager.find(Koulutus.class, koulutus1.getId());
    var updatedKoulutus2 = entityManager.find(Koulutus.class, koulutus2.getId());

    assertEquals(OsaamisenTunnistusStatus.DONE, updatedKoulutus1.getOsaamisenTunnistusStatus());
    assertEquals(1, updatedKoulutus1.getOsaamiset().size());
    assertEquals(OsaamisenTunnistusStatus.FAIL, updatedKoulutus2.getOsaamisenTunnistusStatus());
    assertEquals(0, updatedKoulutus2.getOsaamiset().size());
  }

  @Test
  void shouldAddOsaamisetToKoulutusWhenUpdatingStatus() {
    var koulutus = new Koulutus(entityManager.find(KoulutusKokonaisuus.class, kokonaisuusId));
    koulutus.setOsaamisenTunnistusStatus(OsaamisenTunnistusStatus.WAIT);
    entityManager.persist(koulutus);
    simulateCommit();

    var osaamiset = Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"));
    service.completeOsaamisetTunnistus(koulutus, OsaamisenTunnistusStatus.DONE, osaamiset);

    var updatedKoulutus = entityManager.find(Koulutus.class, koulutus.getId());
    assertEquals(OsaamisenTunnistusStatus.DONE, updatedKoulutus.getOsaamisenTunnistusStatus());
    assertTrue(
        updatedKoulutus.getOsaamiset().stream()
            .map(osaaminen -> osaaminen.getOsaaminen().getUri())
            .collect(Collectors.toSet())
            .containsAll(osaamiset));
  }
}
