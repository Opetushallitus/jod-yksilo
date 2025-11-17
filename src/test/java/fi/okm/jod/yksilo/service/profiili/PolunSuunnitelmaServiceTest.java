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
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.TavoiteTyyppi;
import fi.okm.jod.yksilo.dto.profiili.TavoiteDto;
import fi.okm.jod.yksilo.dto.profiili.suunnitelma.PolunSuunnitelmaDto;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/mahdollisuudet-test-data.sql")
@Import({
  PolunSuunnitelmaService.class,
  TavoiteService.class,
  YksilonOsaaminenService.class,
  MuuOsaaminenService.class
})
class PolunSuunnitelmaServiceTest extends AbstractServiceTest {
  @Autowired private TavoiteService tavoitteet;
  @Autowired private TyomahdollisuusRepository tyomahdollisuudet;
  @Autowired private KoulutusmahdollisuusRepository koulutusmahdollisuudet;
  @Autowired private PolunSuunnitelmaService service;

  @Test
  void shouldAddSuunnitelma() {
    var tavoite = ls("tavoite");
    var tavoiteId = addTavoite(tavoite);
    var suunnitelmaNimi = ls("nimi");
    var dto = new PolunSuunnitelmaDto(null, suunnitelmaNimi, null, null, emptySet(), emptySet());
    var id = assertDoesNotThrow(() -> service.add(user, tavoiteId, dto));
    var result = service.get(user, tavoiteId, id);
    assertThat(dto)
        .usingRecursiveComparison()
        .ignoringFields("id", "tavoite", "vaiheet")
        .isEqualTo(result);
    assertThat(result.nimi()).usingRecursiveComparison().isEqualTo(suunnitelmaNimi);
  }

  @Test
  void shouldUpdateSuunnitelma() {
    var tavoiteId = addTavoite(ls("tavoite"));
    var dto = new PolunSuunnitelmaDto(null, ls("nimi"), null, null, emptySet(), emptySet());
    var id = service.add(user, tavoiteId, dto);
    var updateDto = new PolunSuunnitelmaDto(id, ls("uusi nimi"), null, null, null, null);
    service.update(user, tavoiteId, updateDto);
    assertThat(updateDto)
        .usingRecursiveComparison()
        .ignoringFields("id", "tavoite", "vaiheet", "osaamiset", "ignoredOsaamiset")
        .isEqualTo(service.get(user, tavoiteId, id));
  }

  @Test
  void shouldDeleteSuunnitelma() {
    var tavoiteId = addTavoite(ls("tavoite"));
    var dto = new PolunSuunnitelmaDto(null, ls("nimi"), null, null, emptySet(), emptySet());
    var id = service.add(user, tavoiteId, dto);
    service.delete(user, tavoiteId, id);
    assertThrows(NotFoundException.class, () -> service.get(user, tavoiteId, id));
  }

  @Test
  void shouldThrowServiceValidationExceptionWhenAddingTooManySuunnitelmas() {
    int testLimit = 3;
    try (var mockedService = mockStatic(PolunSuunnitelmaService.class)) {
      mockedService
          .when(PolunSuunnitelmaService::getSuunnitelmaPerTavoiteLimit)
          .thenReturn(testLimit);
      var tavoiteId = addTavoite(ls("tavoite"));

      // Add the maximum allowed number of Suunnitelmas
      for (int i = 0; i < testLimit; i++) {
        var dto = new PolunSuunnitelmaDto(null, ls("nimi" + i), null, null, emptySet(), emptySet());
        service.add(user, tavoiteId, dto);
      }

      // Attempt to add one more Suunnitelma, which should throw an exception
      var dto = new PolunSuunnitelmaDto(null, ls("extra nimi"), null, null, emptySet(), emptySet());
      assertThrows(ServiceValidationException.class, () -> service.add(user, tavoiteId, dto));
    }
  }

  private UUID addTavoite(LocalizedString tavoite) {
    return addTavoite(tavoite, MahdollisuusTyyppi.TYOMAHDOLLISUUS);
  }

  private UUID addTavoite(LocalizedString tavoite, MahdollisuusTyyppi mahdollisuusTyyppi) {
    return tavoitteet.add(
        user,
        new TavoiteDto(
            null,
            TavoiteTyyppi.PITKA,
            mahdollisuusTyyppi,
            MahdollisuusTyyppi.TYOMAHDOLLISUUS.equals(mahdollisuusTyyppi)
                ? tyomahdollisuudet.findAll().getFirst().getId()
                : koulutusmahdollisuudet.findAll().getFirst().getId(),
            tavoite,
            tavoite,
            null,
            null));
  }
}
