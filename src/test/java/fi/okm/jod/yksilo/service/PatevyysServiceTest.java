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

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.service.profiili.PatevyysService;
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

@Import({PatevyysService.class, YksilonOsaaminenService.class})
class PatevyysServiceTest extends AbstractServiceTest {

  @Autowired PatevyysService service;
  private UUID toimintoId;

  @BeforeEach
  public void setUp() {
    var toiminto =
        new Toiminto(
            entityManager.find(Yksilo.class, user.getId()),
            new LocalizedString(Map.of(Kieli.FI, "Testi")));
    this.toimintoId = entityManager.persist(toiminto).getId();
    entityManager.flush();
  }

  @Test
  void shouldAddPatevyys() {
    assertDoesNotThrow(
        () -> {
          service.add(
              user,
              toimintoId,
              new PatevyysDto(
                  null,
                  ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                  null,
                  LocalDate.of(2021, 1, 1),
                  LocalDate.of(2021, 12, 31),
                  Set.of(URI.create("urn:osaaminen1"))));
          entityManager.flush();
          entityManager.clear();

          var result = service.findAll(user, toimintoId);
          assertEquals(1, result.size());
        });
  }

  @Test
  void shouldUpdatePatevyys() {
    assertDoesNotThrow(
        () -> {
          var id =
              service.add(
                  user,
                  toimintoId,
                  new PatevyysDto(
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
              toimintoId,
              new PatevyysDto(
                  id,
                  ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                  null,
                  LocalDate.of(2021, 1, 1),
                  LocalDate.of(2021, 12, 31),
                  updated));
          entityManager.flush();
          entityManager.clear();

          var result = service.get(user, toimintoId, id);
          assertEquals(updated, result.osaamiset());
        });
  }

  @Test
  void shouldDeleteEmptyToiminto() {
    var id =
        service.add(
            user,
            toimintoId,
            new PatevyysDto(
                null,
                ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                null,
                LocalDate.of(2021, 1, 1),
                null,
                Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"))));
    simulateCommit();
    service.delete(user, toimintoId, id);
    simulateCommit();
    assertNull(entityManager.find(Toiminto.class, toimintoId));
  }

  @Test
  void shouldKeepNotEmptyToiminto() {
    service.add(
        user,
        toimintoId,
        new PatevyysDto(
            null,
            ls(Kieli.FI, "nimi1", Kieli.SV, "namn"),
            null,
            LocalDate.of(2021, 1, 1),
            null,
            Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"))));

    var id =
        service.add(
            user,
            toimintoId,
            new PatevyysDto(
                null,
                ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                null,
                LocalDate.of(2021, 1, 1),
                null,
                Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"))));
    simulateCommit();
    service.delete(user, toimintoId, id);
    simulateCommit();
    assertNotNull(entityManager.find(Toiminto.class, toimintoId));
  }
}
