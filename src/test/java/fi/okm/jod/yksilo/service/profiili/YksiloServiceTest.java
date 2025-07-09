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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.PaamaaraTyyppi;
import fi.okm.jod.yksilo.dto.profiili.PaamaaraDto;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/data/mahdollisuudet-test-data.sql"})
@Import({YksiloService.class, PaamaaraService.class})
class YksiloServiceTest extends AbstractServiceTest {
  @Autowired private YksiloService service;
  @Autowired private PaamaaraService paamaaraService;
  @Autowired private TyomahdollisuusRepository tyomahdollisuudet;

  @Test
  void delete() {
    paamaaraService.add(
        user,
        new PaamaaraDto(
            null,
            PaamaaraTyyppi.MUU,
            MahdollisuusTyyppi.TYOMAHDOLLISUUS,
            tyomahdollisuudet.findAll().getFirst().getId(),
            ls("tavoite"),
            null,
            null));
    simulateCommit();
    assertDoesNotThrow(() -> service.delete(user));
    assertEquals(0, paamaaraService.findAll(user).size());
    assertThrows(NotFoundException.class, () -> service.get(user));
  }
}
