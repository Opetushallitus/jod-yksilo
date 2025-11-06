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

import fi.okm.jod.yksilo.config.suomifi.Attribute;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.Sukupuoli;
import fi.okm.jod.yksilo.domain.TavoiteTyyppi;
import fi.okm.jod.yksilo.dto.profiili.TavoiteDto;
import fi.okm.jod.yksilo.dto.profiili.YksiloDto;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/data/mahdollisuudet-test-data.sql"})
@Import({YksiloService.class, TavoiteService.class})
class YksiloServiceTest extends AbstractServiceTest {
  @Autowired private YksiloService service;
  @Autowired private TavoiteService tavoiteService;
  @Autowired private TyomahdollisuusRepository tyomahdollisuudet;

  @Test
  void shouldDeleteUserProfile() {
    tavoiteService.add(
        user,
        new TavoiteDto(
            null,
            TavoiteTyyppi.MUU,
            MahdollisuusTyyppi.TYOMAHDOLLISUUS,
            tyomahdollisuudet.findAll().getFirst().getId(),
            ls("tavoite"),
            ls("kuvaus"),
            null,
            null));
    simulateCommit();
    assertDoesNotThrow(() -> service.delete(user));
    assertEquals(0, tavoiteService.findAll(user).size());
    assertThrows(NotFoundException.class, () -> service.get(user));
  }

  @Test
  void shouldUpdateTiedot() {
    var tiedot =
        new YksiloDto(
            null,
            true,
            true,
            true,
            1999,
            Sukupuoli.NAINEN,
            user.getAttribute(Attribute.KOTIKUNTA_KUNTANUMERO).get(),
            "fi",
            Kieli.EN,
            "user@example.org");
    service.update(user, tiedot);
    assertEquals(tiedot, service.get(user));
  }

  @Test
  void shouldNotChangeIdentificationAttributes() {
    shouldUpdateTiedot();

    var tiedot =
        new YksiloDto(
            null,
            true,
            true,
            true,
            2000,
            Sukupuoli.MIES,
            "200",
            "fi",
            Kieli.EN,
            "user@example.org");
    assertThrows(ServiceValidationException.class, () -> service.update(user, tiedot));
  }
}
