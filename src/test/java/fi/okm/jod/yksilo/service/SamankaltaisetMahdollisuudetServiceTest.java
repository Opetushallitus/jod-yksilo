/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static fi.okm.jod.yksilo.domain.MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS;
import static fi.okm.jod.yksilo.domain.MahdollisuusTyyppi.TYOMAHDOLLISUUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.dto.SamankaltainenMahdollisuusDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(SamankaltaisetMahdollisuudetService.class)
class SamankaltaisetMahdollisuudetServiceTest extends AbstractServiceTest {

  @Autowired private JdbcClient jdbc;
  @Autowired private SamankaltaisetMahdollisuudetService service;

  @Test
  void shouldFindSimilarOfTheSameTypeOrderedByJaccardIndex() {
    var source = tyomahdollisuus(true, "a", "b", "c", "d");
    var identical = tyomahdollisuus(true, "a", "b", "c", "d");
    var mostlyShared = tyomahdollisuus(true, "a", "b", "c", "d", "e", "f");
    var partlyShared = tyomahdollisuus(true, "a", "b", "c", "x", "y");
    tyomahdollisuus(true, "a", "b"); // too few shared osaamiset
    tyomahdollisuus(true, "x", "y", "z"); // nothing shared
    tyomahdollisuus(false, "a", "b", "c", "d"); // inactive

    assertThat(ids(TYOMAHDOLLISUUS, source, TYOMAHDOLLISUUS))
        .containsExactly(identical, mostlyShared, partlyShared);
  }

  @Test
  void shouldFindSimilarKoulutusmahdollisuudetForKoulutusmahdollisuus() {
    var source = koulutusmahdollisuus(true, "a", "b", "c", "d");
    var identical = koulutusmahdollisuus(true, "a", "b", "c", "d");
    var partlyShared = koulutusmahdollisuus(true, "a", "b", "c", "x", "y");
    koulutusmahdollisuus(true, "a", "b");
    koulutusmahdollisuus(false, "a", "b", "c", "d");

    assertThat(ids(KOULUTUSMAHDOLLISUUS, source, KOULUTUSMAHDOLLISUUS))
        .containsExactly(identical, partlyShared);
  }

  @Test
  void shouldFindSimilarKoulutusmahdollisuudetForTyomahdollisuus() {
    var source = tyomahdollisuus(true, "a", "b", "c", "d");
    var best = koulutusmahdollisuus(true, "a", "b", "c", "d");
    var other = koulutusmahdollisuus(true, "a", "b", "c", "x");
    koulutusmahdollisuus(false, "a", "b", "c", "d");
    tyomahdollisuus(true, "a", "b", "c", "d"); // wrong type

    assertThat(ids(TYOMAHDOLLISUUS, source, KOULUTUSMAHDOLLISUUS)).containsExactly(best, other);
  }

  @Test
  void shouldFindSimilarTyomahdollisuudetForKoulutusmahdollisuus() {
    var source = koulutusmahdollisuus(true, "a", "b", "c", "d");
    var best = tyomahdollisuus(true, "a", "b", "c", "d");
    var other = tyomahdollisuus(true, "a", "b", "c", "x");
    tyomahdollisuus(false, "a", "b", "c", "d");
    koulutusmahdollisuus(true, "a", "b", "c", "d"); // wrong type

    assertThat(ids(KOULUTUSMAHDOLLISUUS, source, TYOMAHDOLLISUUS)).containsExactly(best, other);
  }

  @Test
  void shouldReturnTitles() {
    var source = tyomahdollisuus(true, "a", "b", "c");
    var similar = tyomahdollisuus(true, "a", "b", "c");

    var result = service.find(TYOMAHDOLLISUUS, source, TYOMAHDOLLISUUS);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo(similar);
    assertThat(result.getFirst().otsikko().get(Kieli.FI)).isEqualTo("Otsikko " + similar);
  }

  @Test
  void shouldLeaveOutWeakMatchesThatFollowMuchStrongerOnes() {
    var source = tyomahdollisuus(true, "a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
    var strong = tyomahdollisuus(true, "a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
    var medium = tyomahdollisuus(true, "a", "b", "c", "d", "e", "x1", "x2", "x3", "x4", "x5");
    // Shares the minimum number of osaamiset but is only 3 / 24 similar compared to 1.0 of the best
    tyomahdollisuus(
        true, "a", "b", "c", "y1", "y2", "y3", "y4", "y5", "y6", "y7", "y8", "y9", "y10");

    assertThat(ids(TYOMAHDOLLISUUS, source, TYOMAHDOLLISUUS)).containsExactly(strong, medium);
  }

  @Test
  void shouldKeepWeakMatchesWhenNoStrongerOnesExist() {
    var source = tyomahdollisuus(true, "a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
    var weak = tyomahdollisuus(true, "a", "b", "c", "y1", "y2", "y3", "y4", "y5", "y6", "y7");

    assertThat(ids(TYOMAHDOLLISUUS, source, TYOMAHDOLLISUUS)).containsExactly(weak);
  }

  @Test
  void shouldReturnAtMostFiveResults() {
    var source = tyomahdollisuus(true, "a", "b", "c");
    for (int i = 0; i < 7; i++) {
      tyomahdollisuus(true, "a", "b", "c", "x" + i);
    }

    assertThat(ids(TYOMAHDOLLISUUS, source, TYOMAHDOLLISUUS)).hasSize(5);
  }

  @Test
  void shouldCapMinimumOfSharedOsaamisetToSizeOfSource() {
    var source = tyomahdollisuus(true, "a", "b");
    var similar = tyomahdollisuus(true, "a", "b", "c");

    assertThat(ids(TYOMAHDOLLISUUS, source, TYOMAHDOLLISUUS)).containsExactly(similar);
  }

  @Test
  void shouldReturnNothingWhenSourceHasNoOsaamiset() {
    var source = tyomahdollisuus(true);
    tyomahdollisuus(true, "a", "b", "c");
    koulutusmahdollisuus(true, "a", "b", "c");

    assertThat(ids(TYOMAHDOLLISUUS, source, TYOMAHDOLLISUUS)).isEmpty();
    assertThat(ids(TYOMAHDOLLISUUS, source, KOULUTUSMAHDOLLISUUS)).isEmpty();
  }

  @Test
  void shouldIgnoreOtherJakaumaTypes() {
    var source = tyomahdollisuus(true, "a", "b", "c");
    var other = tyomahdollisuus(true, "x", "y", "z");
    insertArvot("tyomahdollisuus", other, "KUNTA", "a", "b", "c");

    assertThat(ids(TYOMAHDOLLISUUS, source, TYOMAHDOLLISUUS)).isEmpty();
  }

  @Test
  void shouldThrowNotFoundForUnknownMahdollisuus() {
    var id = UUID.randomUUID();

    assertThatThrownBy(() -> service.find(TYOMAHDOLLISUUS, id, KOULUTUSMAHDOLLISUUS))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> service.find(KOULUTUSMAHDOLLISUUS, id, TYOMAHDOLLISUUS))
        .isInstanceOf(NotFoundException.class);
  }

  private List<UUID> ids(MahdollisuusTyyppi lahde, UUID id, MahdollisuusTyyppi kohde) {
    return service.find(lahde, id, kohde).stream().map(SamankaltainenMahdollisuusDto::id).toList();
  }

  private UUID tyomahdollisuus(boolean aktiivinen, String... osaamiset) {
    var id = UUID.randomUUID();
    jdbc.sql("INSERT INTO yksilo.tyomahdollisuus (id, aktiivinen) VALUES (:id, :aktiivinen)")
        .param("id", id)
        .param("aktiivinen", aktiivinen)
        .update();
    insertOtsikko("tyomahdollisuus", id);
    if (osaamiset.length > 0) {
      insertArvot("tyomahdollisuus", id, "OSAAMINEN", osaamiset);
    }
    return id;
  }

  private UUID koulutusmahdollisuus(boolean aktiivinen, String... osaamiset) {
    var id = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO yksilo.koulutusmahdollisuus (id, aktiivinen, tyyppi) "
                + "VALUES (:id, :aktiivinen, 'TUTKINTO')")
        .param("id", id)
        .param("aktiivinen", aktiivinen)
        .update();
    insertOtsikko("koulutusmahdollisuus", id);
    if (osaamiset.length > 0) {
      insertArvot("koulutusmahdollisuus", id, "OSAAMINEN", osaamiset);
    }
    return id;
  }

  private void insertOtsikko(String table, UUID id) {
    jdbc.sql(
            "INSERT INTO yksilo.%s_kaannos (%s_id, kaannos_key, otsikko) VALUES (:id, 'FI', :otsikko)"
                .formatted(table, table))
        .param("id", id)
        .param("otsikko", "Otsikko " + id)
        .update();
  }

  private void insertArvot(String table, UUID id, String tyyppi, String... arvot) {
    var jakaumaId =
        jdbc.sql(
                "INSERT INTO yksilo.%s_jakauma (%s_id, tyyppi, maara, tyhjia) "
                        .formatted(table, table)
                    + "VALUES (:id, :tyyppi, 1, 0) RETURNING id")
            .param("id", id)
            .param("tyyppi", tyyppi)
            .query(Long.class)
            .single();
    for (var arvo : arvot) {
      jdbc.sql(
              "INSERT INTO yksilo.%s_jakauma_arvot (%s_jakauma_id, arvo, osuus) "
                      .formatted(table, table)
                  + "VALUES (:jakaumaId, :arvo, 1.0)")
          .param("jakaumaId", jakaumaId)
          .param("arvo", arvo)
          .update();
    }
  }
}
