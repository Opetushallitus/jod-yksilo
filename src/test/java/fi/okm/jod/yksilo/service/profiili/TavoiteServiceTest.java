/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.dto.profiili.TavoiteDto;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.NotFoundException;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/mahdollisuudet-test-data.sql")
@Import(TavoiteService.class)
class TavoiteServiceTest extends AbstractServiceTest {
  @Autowired private TavoiteService service;
  @Autowired private TyomahdollisuusRepository tyomahdollisuudet;

  @Test
  void shouldAddTavoite() {
    var dto =
        new TavoiteDto(
            null,
            MahdollisuusTyyppi.TYOMAHDOLLISUUS,
            tyomahdollisuudet.findAll().getFirst().getId(),
            ls("tavoite"),
            ls("kuvaus"),
            null,
            null);
    assertDoesNotThrow(() -> service.add(user, dto));
    assertEquals(1, service.findAll(user).size());
  }

  @Test
  void shouldUpdateTavoite() {
    var dto =
        new TavoiteDto(
            null,
            MahdollisuusTyyppi.TYOMAHDOLLISUUS,
            tyomahdollisuudet.findAll().getFirst().getId(),
            ls("tavoite"),
            ls("kuvaus2"),
            null,
            null);

    var id = service.add(user, dto);
    var updated =
        new TavoiteDto(
            id,
            dto.mahdollisuusTyyppi(),
            dto.mahdollisuusId(),
            ls("tavoite2"),
            ls("kuvaus2"),
            null,
            new HashSet<>());
    assertDoesNotThrow(() -> service.update(user, updated));
    assertThat(updated)
        .usingRecursiveComparison()
        .ignoringFields("luotu")
        .isEqualTo(service.findAll(user).getFirst());
  }

  @Test
  void shouldNotAddInvalidTavoite() {
    var dto =
        new TavoiteDto(
            null,
            MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS,
            UUID.randomUUID(),
            ls("tavoite"),
            ls("kuvaus"),
            null,
            null);
    assertThrows(NotFoundException.class, () -> service.add(user, dto));
  }

  @Test
  void shouldDeleteTavoite() {
    var dto =
        new TavoiteDto(
            null,
            MahdollisuusTyyppi.TYOMAHDOLLISUUS,
            tyomahdollisuudet.findAll().getFirst().getId(),
            ls("tavoite"),
            ls("kuvaus"),
            null,
            null);
    var id = service.add(user, dto);
    assertDoesNotThrow(() -> service.delete(user, id));
    assertEquals(0, service.findAll(user).size());
  }

  @Test
  void shouldNotDeleteOtherUserTavoite() {
    var dto =
        new TavoiteDto(
            null,
            MahdollisuusTyyppi.TYOMAHDOLLISUUS,
            tyomahdollisuudet.findAll().getFirst().getId(),
            ls("tavoite"),
            ls("kuvaus"),
            null,
            null);
    var id = service.add(user, dto);
    assertThrows(NotFoundException.class, () -> service.delete(user2, id));
    assertEquals(1, service.findAll(user).size());
  }
}
