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

import fi.okm.jod.yksilo.controller.ehdotus.Suggestion;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import java.net.URI;
import java.util.List;
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
@Sql(
    value = {
      "/data/osaaminen.sql",
      "/data/koulutusmahdollisuudet.sql",
      "/data/tyomahdollisuudet.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Import(MahdollisuudetService.class)
class MahdollisuudetServiceTest extends AbstractServiceTest {

  private static final UUID koulutusIdActive1 =
      UUID.fromString("481e204a-691a-48dd-9b01-7f08d5858db9"); // Have osaamiset 1 to 2. (2 pc)
  private static final UUID koulutusIdActive2 =
      UUID.fromString("c11249fd-e0a3-4b23-8de5-9dc67a157f46"); // Have osaamiset 1 to 6. (6 pc)
  private static final UUID koulutusIdInactive =
      UUID.fromString("c74eed41-c729-433e-8d36-4fc7527fe3df");
  private static final UUID tyoIdActive1 =
      UUID.fromString("ca466237-ce1d-4aca-9f9b-2ed566ef4f94"); // Have osaamiset 1 to 3. (3 pc)
  private static final UUID tyoIdActive2 =
      UUID.fromString("cd5bb0b2-d09d-45e0-96e5-6c0c9a37a949"); // Have osaamiset 2 and 3. (2 pc)
  private static final UUID tyoIdInactive = UUID.fromString("af34f11f-05b5-434c-963a-df6d89a2149b");

  @Autowired private MahdollisuudetService mahdollisuudetService;

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
        .containsEntry(koulutusIdActive1, MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS)
        .containsEntry(koulutusIdActive2, MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS)
        .containsEntry(tyoIdActive1, MahdollisuusTyyppi.TYOMAHDOLLISUUS)
        .doesNotContainKeys(koulutusIdInactive, tyoIdInactive);
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
    var result = mahdollisuudetService.getMahdollisuudetSuggestionsForPolkuVaihe(Set.of());
    assertThat(result).isEmpty();
  }

  @Test
  void shouldGetMahdollisuudetSuggestionsForPolkuVaihe_withMatchingMissingOsaamiset() {
    var matchingMissingOsaamiset =
        Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"));

    var result =
        mahdollisuudetService.getMahdollisuudetSuggestionsForPolkuVaihe(matchingMissingOsaamiset);

    var expectedOrderedKeys =
        List.of(koulutusIdActive1, tyoIdActive1, tyoIdActive2, koulutusIdActive2);
    assertThat(result).isNotEmpty().hasSize(expectedOrderedKeys.size());
    var resultIds = result.stream().map(Suggestion::id).toList();
    assertThat(resultIds)
        .containsExactlyElementsOf(expectedOrderedKeys)
        .doesNotContain(koulutusIdInactive, tyoIdInactive);
  }

  @Test
  void shouldGetMahdollisuudetSuggestionsForPolkuVaihe_noMatches() {
    var nonMatchingOsaamiset = Set.of(URI.create("nonexistent1"), URI.create("nonexistent2"));
    var result =
        mahdollisuudetService.getMahdollisuudetSuggestionsForPolkuVaihe(nonMatchingOsaamiset);
    assertThat(result).isEmpty();
  }
}
