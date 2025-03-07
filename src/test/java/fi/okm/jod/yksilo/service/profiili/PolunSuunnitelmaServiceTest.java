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

import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.PaamaaraTyyppi;
import fi.okm.jod.yksilo.dto.profiili.PaamaaraDto;
import fi.okm.jod.yksilo.dto.profiili.PolunSuunnitelmaDto;
import fi.okm.jod.yksilo.dto.profiili.PolunSuunnitelmaUpdateDto;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/osaaminen.sql")
@Sql("/data/mahdollisuudet-test-data.sql")
@Import({PolunSuunnitelmaService.class, PaamaaraService.class, YksilonOsaaminenService.class})
public class PolunSuunnitelmaServiceTest extends AbstractServiceTest {
  @Autowired private PaamaaraService paamaarat;
  @Autowired private TyomahdollisuusRepository tyomahdollisuusdet;
  @Autowired private PolunSuunnitelmaService service;

  @Test
  void shouldAddSuunnitelma() {
    var tavoite = ls("tavoite");
    var paamaaraId =
        paamaarat.add(
            user,
            new PaamaaraDto(
                null,
                PaamaaraTyyppi.PITKA,
                MahdollisuusTyyppi.TYOMAHDOLLISUUS,
                tyomahdollisuusdet.findAll().getFirst().getId(),
                tavoite,
                null,
                null));
    var dto = new PolunSuunnitelmaDto(null, ls("nimi"), null, null, emptySet(), emptySet());
    var id = assertDoesNotThrow(() -> service.add(user, paamaaraId, dto));
    var result = service.get(user, paamaaraId, id);
    assertThat(dto)
        .usingRecursiveComparison()
        .ignoringFields("id", "paamaara", "vaiheet")
        .isEqualTo(service.get(user, paamaaraId, id));
    assertThat(result.paamaara().id()).isEqualTo(paamaaraId);
    assertThat(result.paamaara().tavoite()).usingRecursiveComparison().isEqualTo(tavoite);
  }

  @Test
  void shouldUpdateSuunnitelma() {
    var paamaaraId =
        paamaarat.add(
            user,
            new PaamaaraDto(
                null,
                PaamaaraTyyppi.PITKA,
                MahdollisuusTyyppi.TYOMAHDOLLISUUS,
                tyomahdollisuusdet.findAll().getFirst().getId(),
                ls("tavoite"),
                null,
                null));
    var dto = new PolunSuunnitelmaDto(null, ls("nimi"), null, null, emptySet(), emptySet());
    var id = service.add(user, paamaaraId, dto);
    var updateDto = new PolunSuunnitelmaUpdateDto(id, ls("uusi nimi"), null, null);
    service.update(user, paamaaraId, updateDto);
    assertThat(updateDto)
        .usingRecursiveComparison()
        .ignoringFields("id", "paamaara", "vaiheet", "osaamiset", "ignoredOsaamiset")
        .isEqualTo(service.get(user, paamaaraId, id));
  }

  @Test
  void shouldDeleteSuunnitelma() {
    var paamaaraId =
        paamaarat.add(
            user,
            new PaamaaraDto(
                null,
                PaamaaraTyyppi.PITKA,
                MahdollisuusTyyppi.TYOMAHDOLLISUUS,
                tyomahdollisuusdet.findAll().getFirst().getId(),
                ls("tavoite"),
                null,
                null));
    var dto = new PolunSuunnitelmaDto(null, ls("nimi"), null, null, emptySet(), emptySet());
    var id = service.add(user, paamaaraId, dto);
    service.delete(user, paamaaraId, id);
    assertThrows(NotFoundException.class, () -> service.get(user, paamaaraId, id));
  }

  @Test
  void shouldThrowServiceValidationExceptionWhenAddingTooManySuunnitelmas() {
    int testLimit = 3;
    try (var mockedService = mockStatic(PolunSuunnitelmaService.class)) {
      mockedService
          .when(PolunSuunnitelmaService::getSuunnitelmaPerPaamaaraLimit)
          .thenReturn(testLimit);
      var paamaaraId =
          paamaarat.add(
              user,
              new PaamaaraDto(
                  null,
                  PaamaaraTyyppi.PITKA,
                  MahdollisuusTyyppi.TYOMAHDOLLISUUS,
                  tyomahdollisuusdet.findAll().getFirst().getId(),
                  ls("tavoite"),
                  null,
                  null));

      // Add the maximum allowed number of Suunnitelmas
      for (int i = 0; i < testLimit; i++) {
        var dto = new PolunSuunnitelmaDto(null, ls("nimi" + i), null, null, emptySet(), emptySet());
        service.add(user, paamaaraId, dto);
      }

      // Attempt to add one more Suunnitelma, which should throw an exception
      var dto = new PolunSuunnitelmaDto(null, ls("extra nimi"), null, null, emptySet(), emptySet());
      assertThrows(ServiceValidationException.class, () -> service.add(user, paamaaraId, dto));
    }
  }
}
