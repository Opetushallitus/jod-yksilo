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
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.service.profiili.ToimenkuvaService;
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

@Import({ToimenkuvaService.class, YksilonOsaaminenService.class})
class ToimenkuvaServiceTest extends AbstractServiceTest {

  @Autowired ToimenkuvaService service;
  private UUID tyopaikkaId;

  @BeforeEach
  public void setUp() {
    var tyopaikka =
        new Tyopaikka(
            entityManager.find(Yksilo.class, user.getId()),
            new LocalizedString(Map.of(Kieli.FI, "Testi")));
    this.tyopaikkaId = entityManager.persist(tyopaikka).getId();
    entityManager.flush();
  }

  @Test
  void shouldAddToimenkuva() {
    assertDoesNotThrow(
        () -> {
          service.add(
              user,
              tyopaikkaId,
              new ToimenkuvaDto(
                  null,
                  ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                  null,
                  LocalDate.of(2021, 1, 1),
                  LocalDate.of(2021, 12, 31),
                  Set.of(URI.create("urn:osaaminen1"))));
          entityManager.flush();
          entityManager.clear();

          var result = service.findAll(user, tyopaikkaId);
          assertEquals(1, result.size());
        });
  }

  @Test
  void shouldUpdateToimenkuva() {
    assertDoesNotThrow(
        () -> {
          var id =
              service.add(
                  user,
                  tyopaikkaId,
                  new ToimenkuvaDto(
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
              tyopaikkaId,
              new ToimenkuvaDto(
                  id,
                  ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                  null,
                  LocalDate.of(2021, 1, 1),
                  LocalDate.of(2021, 12, 31),
                  updated));
          entityManager.flush();
          entityManager.clear();

          var result = service.get(user, tyopaikkaId, id);
          assertEquals(updated, result.osaamiset());
        });
  }

  @Test
  void shouldDeleteEmptyTyopaikka() {
    var id =
        service.add(
            user,
            tyopaikkaId,
            new ToimenkuvaDto(
                null,
                ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                null,
                LocalDate.of(2021, 1, 1),
                null,
                Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"))));
    simulateCommit();
    service.delete(user, tyopaikkaId, id);
    simulateCommit();
    assertNull(entityManager.find(Tyopaikka.class, tyopaikkaId));
  }

  @Test
  void shouldKeepNotEmptyTyopaikka() {
    service.add(
        user,
        tyopaikkaId,
        new ToimenkuvaDto(
            null,
            ls(Kieli.FI, "nimi1", Kieli.SV, "namn"),
            null,
            LocalDate.of(2021, 1, 1),
            null,
            Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"))));

    var id =
        service.add(
            user,
            tyopaikkaId,
            new ToimenkuvaDto(
                null,
                ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                null,
                LocalDate.of(2021, 1, 1),
                null,
                Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"))));
    simulateCommit();
    service.delete(user, tyopaikkaId, id);
    simulateCommit();
    assertNotNull(entityManager.find(Tyopaikka.class, tyopaikkaId));
  }
}
