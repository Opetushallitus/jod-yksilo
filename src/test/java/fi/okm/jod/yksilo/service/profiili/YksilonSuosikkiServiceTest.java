/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.dto.profiili.SuosikkiDto;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import java.util.UUID;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;

@Import({YksilonSuosikkiService.class})
@Sql("/data/create/yksilon-suosikki-test-data.sql")
class YksilonSuosikkiServiceTest extends AbstractServiceTest {

  private static final UUID[] TEST_IDS = {
    UUID.fromString("00016334-886e-4d11-93f0-872fcf671920"),
    UUID.fromString("00143beb-0817-4e6d-9107-57d0245b57ee"),
    UUID.fromString("0014885a-4aa6-4202-9865-2fcb4457cc59")
  };

  @Autowired YksilonSuosikkiService service;

  @Test
  @WithMockUser
  void testAddingAndGettingYksilonSuosikkit() throws AssertionFailure {
    var newID = service.create(user, TEST_IDS[0], SuosikkiTyyppi.TYOMAHDOLLISUUS);
    var suosikit = service.findAll(user, null);
    assertEquals(1L, suosikit.size());
    var suosikki = suosikit.getFirst();
    assertNotNull(suosikki.id());
    assertEquals(newID, suosikki.id());
    assertNotNull(suosikki.luotu());
    assertEquals(TEST_IDS[0], suosikki.suosionKohdeId());
    assertEquals(SuosikkiTyyppi.TYOMAHDOLLISUUS, suosikki.tyyppi());
  }

  @Test
  @WithMockUser()
  void testAddingSameAgainAndGettingYksilonSuosikkit() throws AssertionFailure {
    var newID1 = service.create(user, TEST_IDS[1], SuosikkiTyyppi.TYOMAHDOLLISUUS);
    var newID2 = service.create(user, TEST_IDS[1], SuosikkiTyyppi.TYOMAHDOLLISUUS);
    assertEquals(newID1, newID2);
    var suosikit = service.findAll(user, null);
    assertEquals(1L, suosikit.size());
  }

  @Test
  @WithMockUser()
  void testDeleteYksilonSuosikkit() throws AssertionFailure {
    var newID1 = service.create(user, TEST_IDS[0], SuosikkiTyyppi.TYOMAHDOLLISUUS);
    var newID2 = service.create(user, TEST_IDS[1], SuosikkiTyyppi.TYOMAHDOLLISUUS);
    var newID3 = service.create(user, TEST_IDS[2], SuosikkiTyyppi.TYOMAHDOLLISUUS);
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
}
