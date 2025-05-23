/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.dto.profiili.SuosikkiDto;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import java.util.List;
import java.util.UUID;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;

@Import({YksilonSuosikkiService.class})
@Sql("/data/mahdollisuudet-test-data.sql")
class YksilonSuosikkiServiceTest extends AbstractServiceTest {

  @Autowired YksilonSuosikkiService service;
  @Autowired TyomahdollisuusRepository tyomahdollisuusRepository;
  @Autowired KoulutusmahdollisuusRepository koulutusmahdollisuusRepository;

  private List<UUID> tyomahdollisuusIds;
  private List<UUID> koulutusmahdollisuusIds;

  @BeforeEach
  void setUp() {
    tyomahdollisuusIds =
        tyomahdollisuusRepository.findAll().stream().map(Tyomahdollisuus::getId).toList();
    koulutusmahdollisuusIds =
        koulutusmahdollisuusRepository.findAll().stream().map(Koulutusmahdollisuus::getId).toList();
  }

  @Test
  @WithMockUser
  void testAdd() throws AssertionFailure {

    final UUID kohdeId = tyomahdollisuusIds.getFirst();
    var newID = service.add(user, kohdeId, SuosikkiTyyppi.TYOMAHDOLLISUUS);
    var suosikit = service.findAll(user, null);

    assertEquals(1L, suosikit.size());
    var suosikki = suosikit.getFirst();
    assertNotNull(suosikki.id());
    assertEquals(newID, suosikki.id());
    assertNotNull(suosikki.luotu());
    assertEquals(kohdeId, suosikki.kohdeId());
    assertEquals(SuosikkiTyyppi.TYOMAHDOLLISUUS, suosikki.tyyppi());
  }

  @Test
  @WithMockUser()
  void testAddingSameAgain() throws AssertionFailure {
    var newID1 = service.add(user, tyomahdollisuusIds.getFirst(), SuosikkiTyyppi.TYOMAHDOLLISUUS);
    var newID2 = service.add(user, tyomahdollisuusIds.getFirst(), SuosikkiTyyppi.TYOMAHDOLLISUUS);
    assertEquals(newID1, newID2);
    var suosikit = service.findAll(user, null);
    assertEquals(1L, suosikit.size());
  }

  @Test
  @WithMockUser()
  void testDelete() throws AssertionFailure {
    var newID1 = service.add(user, tyomahdollisuusIds.get(0), SuosikkiTyyppi.TYOMAHDOLLISUUS);
    var newID2 = service.add(user, tyomahdollisuusIds.get(1), SuosikkiTyyppi.TYOMAHDOLLISUUS);
    var newID3 =
        service.add(user, koulutusmahdollisuusIds.get(0), SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS);
    var suosikitBeforeDelete = service.findAll(user, null);
    assertEquals(3L, suosikitBeforeDelete.size());
    service.delete(user, newID2);
    var suosikitAfterDelete = service.findAll(user, null);
    assertEquals(2L, suosikitAfterDelete.size());
    var ids = suosikitAfterDelete.stream().map(SuosikkiDto::id).toList();
    assertTrue(ids.contains(newID1), "result contains new ID 1");
    assertTrue(ids.contains(newID3), "result contains new ID 2");
    assertFalse(ids.contains(newID2), "result does not contain removed ID");
  }

  @Test
  @WithMockUser
  void testAddWithWrongType() throws AssertionFailure {
    assertThrows(
        ServiceValidationException.class,
        () ->
            service.add(user, tyomahdollisuusIds.getFirst(), SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS));
  }

  @Test
  @WithMockUser
  void testAddNonexistentSuosikki() throws AssertionFailure {
    assertThrows(
        ServiceValidationException.class,
        () -> service.add(user, UUID.randomUUID(), SuosikkiTyyppi.TYOMAHDOLLISUUS));
  }

  @Test
  @WithMockUser
  void testFindAllWithType() throws AssertionFailure {
    service.add(user, tyomahdollisuusIds.get(0), SuosikkiTyyppi.TYOMAHDOLLISUUS);
    service.add(user, tyomahdollisuusIds.get(1), SuosikkiTyyppi.TYOMAHDOLLISUUS);
    var id =
        service.add(user, koulutusmahdollisuusIds.getFirst(), SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS);
    var suosikit = service.findAll(user, SuosikkiTyyppi.TYOMAHDOLLISUUS);
    assertEquals(2L, suosikit.size());
    assertFalse(suosikit.stream().anyMatch(s -> id.equals(s.id())));
  }

  @Test
  @WithMockUser
  void testDeleteNonexistentSuosikki() throws AssertionFailure {
    assertThrows(NotFoundException.class, () -> service.delete(user, UUID.randomUUID()));
  }
}
