/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.ehdotus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Sql(value = {"/data/koulutusmahdollisuudet.sql", "/data/tyomahdollisuudet.sql"})
@Import(MahdollisuudetService.class)
class MahdollisuudetServiceTest extends AbstractServiceTest {

  private static final UUID koulutusIdActive1 =
      UUID.fromString("481e204a-691a-48dd-9b01-7f08d5858db9"); // Have osaamiset 1 to 2. (2 pc)
  private static final UUID koulutusIdActive2 =
      UUID.fromString("c11249fd-e0a3-4b23-8de5-9dc67a157f46"); // Have osaamiset 1 to 6. (6 pc)
  private static final UUID koulutusIdInactive =
      UUID.fromString("c74eed41-c729-433e-8d36-4fc7527fe3df");
  private static final UUID tyoIdInactive = UUID.fromString("af34f11f-05b5-434c-963a-df6d89a2149b");

  @Autowired private MahdollisuudetService mahdollisuudetService;
  @Autowired private KoulutusmahdollisuusRepository koulutusmahdollisuudet;
  @Autowired private TyomahdollisuusRepository tyomahdollisuudet;

  @SuppressWarnings("java:S2699")
  @Test
  void shouldClearCacheAtStartup() {
    mahdollisuudetService.clearCacheAtStartup();
    // Note: This just verifies the method executes, as it's just clearing the cache
  }

  @Test
  void shouldFetchTyoAndKoulutusMahdollisuusIdsWithTypes() {
    var result =
        mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            Sort.Direction.ASC, Kieli.FI);

    assertThat(result)
        .as("Only actives should be included.")
        .hasSize(4)
        .doesNotContainKeys(koulutusIdInactive, tyoIdInactive);
  }

  @Test
  void shouldHaveAlphabeticalOrder() {
    var result =
        mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            Sort.Direction.ASC, Kieli.FI);

    var k = koulutusmahdollisuudet.findById(result.firstEntry().getKey());
    assertThat(k).isPresent();
    assertThat(k.get().getOtsikko().get(Kieli.FI)).isEqualTo("Koulutusmahdollisuus 1");

    var t = tyomahdollisuudet.findById(result.lastEntry().getKey());
    assertThat(t).isPresent();
    assertThat(t.get().getOtsikko().get(Kieli.FI)).isEqualTo("Työmahdollisuus 2");
  }

  @Order(value = 999)
  @Test
  void shouldFetchTyoAndKoulutusMahdollisuusIdsWithTypes_emptyResults() {
    var em = entityManager.getEntityManager();
    em.createNativeQuery("DELETE FROM koulutusmahdollisuus_kaannos WHERE kaannos_key = 'EN'")
        .executeUpdate();
    em.createNativeQuery("DELETE FROM tyomahdollisuus_kaannos WHERE kaannos_key = 'EN'")
        .executeUpdate();
    entityManager.flush();

    var result =
        mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            Sort.Direction.ASC, Kieli.EN);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldGetMahdollisuudetSuggestionsForPolkuVaihe_emptyInput() {
    var result = mahdollisuudetService.getPolkuVaiheSuggestions(Set.of());
    assertThat(result).isEmpty();
  }

  @Test
  void shouldGetMahdollisuudetSuggestionsForPolkuVaihe_withMatchingMissingOsaamiset() {
    var matchingMissingOsaamiset =
        Set.of(URI.create("urn:osaaminen:1"), URI.create("urn:osaaminen:2"));

    var result = mahdollisuudetService.getPolkuVaiheSuggestions(matchingMissingOsaamiset);
    assertThat(result).hasSize(2);

    assertThat(result.get(0).mahdollisuusId()).isEqualTo(koulutusIdActive1);
    assertThat(result.get(1).mahdollisuusId()).isEqualTo(koulutusIdActive2);

    assertPisteet(result.get(0).pisteet(), koulutusIdActive1, matchingMissingOsaamiset);
    assertPisteet(result.get(1).pisteet(), koulutusIdActive2, matchingMissingOsaamiset);
  }

  private void assertPisteet(double pisteet, UUID mahdollisuusId, Set<URI> missingOsaamiset) {

    var mahdollisuus = koulutusmahdollisuudet.findById(mahdollisuusId).orElseThrow();
    var osaamiset =
        mahdollisuus.getJakaumat().get(KoulutusmahdollisuusJakaumaTyyppi.OSAAMINEN).getArvot();
    var matchingOsaamiset =
        osaamiset.stream()
            .filter(osaaminen -> missingOsaamiset.contains(URI.create(osaaminen.arvo())))
            .count();
    // pisteet = matching missing osaamiset / total osaamiset in KoulutusMahdollisuus (6)
    var expected = (double) matchingOsaamiset / osaamiset.size();
    // use approximately equal comparison to account for possible floating point precision issues
    assertThat(pisteet).isCloseTo(expected, within(0.01));
  }

  @Test
  void shouldGetMahdollisuudetSuggestionsForPolkuVaihe_noMatches() {
    var nonMatchingOsaamiset = Set.of(URI.create("nonexistent1"), URI.create("nonexistent2"));
    var result = mahdollisuudetService.getPolkuVaiheSuggestions(nonMatchingOsaamiset);
    assertThat(result).isEmpty();
  }
}
