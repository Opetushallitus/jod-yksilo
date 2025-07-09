/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static fi.okm.jod.yksilo.service.profiili.Mapper.mapPolunVaihe;
import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.PaamaaraTyyppi;
import fi.okm.jod.yksilo.domain.PolunVaiheLahde;
import fi.okm.jod.yksilo.domain.PolunVaiheTyyppi;
import fi.okm.jod.yksilo.dto.profiili.PaamaaraDto;
import fi.okm.jod.yksilo.dto.profiili.PolunSuunnitelmaDto;
import fi.okm.jod.yksilo.dto.profiili.PolunSuunnitelmaUpdateDto;
import fi.okm.jod.yksilo.dto.profiili.PolunVaiheDto;
import fi.okm.jod.yksilo.repository.PolunVaiheRepository;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import java.net.URI;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql(value = {"/data/mahdollisuudet-test-data.sql"})
@Import({
  PolunVaiheService.class,
  PolunSuunnitelmaService.class,
  PaamaaraService.class,
  YksilonOsaaminenService.class,
  MuuOsaaminenService.class
})
class PolunVaiheServiceTest extends AbstractServiceTest {
  @Autowired private PaamaaraService paamaarat;
  @Autowired private TyomahdollisuusRepository tyomahdollisuudet;
  @Autowired private PolunSuunnitelmaService suunnitelmat;
  @Autowired private PolunVaiheService vaiheet;
  @Autowired private PolunVaiheRepository vaiheRepository;

  @Test
  void shouldAddVaihe() {
    var paamaaraId = addPaamaara();
    var suunnitelmaId = addSuunnitelma(paamaaraId);
    var dto =
        createPolunVaiheDto(Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2")));
    var id = vaiheet.add(user, paamaaraId, suunnitelmaId, dto);
    assertThat(dto)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(mapPolunVaihe(vaiheRepository.findById(id).orElseThrow()));
  }

  @Test
  void shouldUpdateVaihe() {
    var paamaaraId = addPaamaara();
    var suunnitelmaId = addSuunnitelma(paamaaraId);
    var dto =
        createPolunVaiheDto(Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2")));
    var id = vaiheet.add(user, paamaaraId, suunnitelmaId, dto);
    var updatedDto =
        new PolunVaiheDto(
            id,
            PolunVaiheLahde.KAYTTAJA,
            null,
            PolunVaiheTyyppi.TYO,
            ls("uusi nimi"),
            ls("uusi kuvaus"),
            Set.of("https://example.com"),
            LocalDate.of(2020, 10, 31),
            LocalDate.of(2023, 2, 5),
            Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2")),
            false);
    vaiheet.update(user, paamaaraId, suunnitelmaId, updatedDto);
    assertThat(updatedDto)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(mapPolunVaihe(vaiheRepository.findById(id).orElseThrow()));
  }

  @Test
  void shouldDeleteVaihe() {
    var paamaaraId = addPaamaara();
    var suunnitelmaId = addSuunnitelma(paamaaraId);
    var dto =
        createPolunVaiheDto(Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2")));
    var id = vaiheet.add(user, paamaaraId, suunnitelmaId, dto);
    vaiheet.delete(user, paamaaraId, suunnitelmaId, id);
    assertThat(vaiheRepository.findById(id)).isEmpty();
    assertThrows(
        NotFoundException.class, () -> vaiheet.delete(user, paamaaraId, suunnitelmaId, id));
  }

  @Test
  void shouldThrowServiceValidationExceptionWhenAddingTooManyVaihes() {
    int testLimit = 3;
    try (MockedStatic<PolunVaiheService> mockedService = mockStatic(PolunVaiheService.class)) {
      mockedService.when(PolunVaiheService::getVaihePerSuunnitelmaLimit).thenReturn(testLimit);

      var paamaaraId = addPaamaara();
      var suunnitelmaId = addSuunnitelma(paamaaraId);
      for (var i = 0; i < testLimit; i++) {
        var dto =
            createPolunVaiheDto(Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2")));
        vaiheet.add(user, paamaaraId, suunnitelmaId, dto);
      }
      var dto =
          createPolunVaiheDto(Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2")));
      assertThrows(
          ServiceValidationException.class,
          () -> vaiheet.add(user, paamaaraId, suunnitelmaId, dto));
    }
  }

  @Test
  void shouldThrowServiceValidationExceptionWhenAddingVaiheWithInvalidOsaaminen() {
    var paamaaraId = addPaamaara();
    var suunnitelmaId = addSuunnitelma(paamaaraId);
    var dto = createPolunVaiheDto(Set.of(URI.create("urn:osaaminen1"), URI.create("urn:unknown")));
    assertThrows(
        ServiceValidationException.class, () -> vaiheet.add(user, paamaaraId, suunnitelmaId, dto));
  }

  @Test
  void shouldThrowServiceValidationExceptionWhenAddingVaiheWithOsaaminenNotInPaamaara() {
    var paamaaraId = addPaamaara();
    var suunnitelmaId = addSuunnitelma(paamaaraId);
    var dto =
        new PolunVaiheDto(
            null,
            PolunVaiheLahde.EHDOTUS,
            UUID.randomUUID(),
            PolunVaiheTyyppi.KOULUTUS,
            ls("nimi"),
            ls("kuvaus"),
            Set.of("https://example.com"),
            LocalDate.of(2020, 10, 31),
            LocalDate.of(2023, 2, 5),
            Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen7")),
            false);
    assertThrows(
        ServiceValidationException.class, () -> vaiheet.add(user, paamaaraId, suunnitelmaId, dto));
  }

  @Test
  void shouldThrowServiceValidationExceptionWhenAddingVaiheWithOsaaminenInSuunnitelma() {
    var paamaaraId = addPaamaara();
    var suunnitelmaId = addSuunnitelma(paamaaraId);
    suunnitelmat.update(
        user,
        paamaaraId,
        new PolunSuunnitelmaUpdateDto(
            suunnitelmaId, ls("nimi"), Set.of(URI.create("urn:osaaminen1")), null));
    var dto = createPolunVaiheDto(Set.of(URI.create("urn:osaaminen1")));
    assertThrows(
        ServiceValidationException.class, () -> vaiheet.add(user, paamaaraId, suunnitelmaId, dto));
  }

  private UUID addPaamaara() {
    return paamaarat.add(
        user,
        new PaamaaraDto(
            null,
            PaamaaraTyyppi.PITKA,
            MahdollisuusTyyppi.TYOMAHDOLLISUUS,
            tyomahdollisuudet.findAll().getFirst().getId(),
            ls("tavoite"),
            null,
            null));
  }

  private UUID addSuunnitelma(UUID paamaaraId) {
    return suunnitelmat.add(
        user,
        paamaaraId,
        new PolunSuunnitelmaDto(null, ls("nimi"), null, emptySet(), emptySet(), emptySet()));
  }

  private PolunVaiheDto createPolunVaiheDto(Set<URI> osaamiset) {
    return new PolunVaiheDto(
        null,
        PolunVaiheLahde.KAYTTAJA,
        null,
        PolunVaiheTyyppi.KOULUTUS,
        ls("nimi"),
        ls("kuvaus"),
        Set.of("https://example.com"),
        LocalDate.of(2020, 10, 31),
        LocalDate.of(2023, 2, 5),
        osaamiset,
        false);
  }
}
