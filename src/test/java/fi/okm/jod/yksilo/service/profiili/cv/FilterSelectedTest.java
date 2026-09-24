/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import static fi.okm.jod.yksilo.service.profiili.cv.CvService.filterSelected;
import static org.assertj.core.api.Assertions.assertThat;

import fi.okm.jod.yksilo.dto.profiili.CvTehtavaSaveDto;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FilterSelectedTest {

  // Simple test records to exercise the generic helper
  record Child(UUID id, String value, Set<URI> osaamiset) {}

  record Parent(UUID id, String name, Set<Child> children) {}

  private static final Function<Parent, UUID> GET_ID = Parent::id;
  private static final Function<Parent, Set<Child>> GET_CHILDREN = Parent::children;
  private static final Function<Child, UUID> GET_CHILD_ID = Child::id;
  private static final Function<Child, Set<URI>> GET_OSAAMISET = Child::osaamiset;
  private static final BiFunction<Child, Set<URI>, Child> WITH_FILTERED_OSAAMISET =
      (c, osaamiset) -> new Child(c.id(), c.value(), osaamiset);
  private static final BiFunction<Parent, Set<Child>, Parent> WITH_FILTERED =
      (p, filtered) -> new Parent(p.id(), p.name(), filtered);

  private static CvTehtavaSaveDto.Lapsi lapsi(UUID id, URI... osaamiset) {
    return new CvTehtavaSaveDto.Lapsi(id, Set.of(osaamiset));
  }

  private static URI osaaminen(UUID id) {
    return URI.create("http://data.europa.eu/esco/skill/" + id);
  }

  @Test
  void shouldSelectMatchingItems() {
    var id1 = UUID.randomUUID();
    var id2 = UUID.randomUUID();
    var id3 = UUID.randomUUID();
    var childId1 = UUID.randomUUID();
    var childId3 = UUID.randomUUID();
    var osaaminenId1 = UUID.randomUUID();
    var osaaminenId3 = UUID.randomUUID();

    var items =
        List.of(
            new Parent(
                id1, "A", Set.of(new Child(childId1, "c1", Set.of(osaaminen(osaaminenId1))))),
            new Parent(
                id2,
                "B",
                Set.of(new Child(UUID.randomUUID(), "c2", Set.of(osaaminen(UUID.randomUUID()))))),
            new Parent(
                id3, "C", Set.of(new Child(childId3, "c3", Set.of(osaaminen(osaaminenId3))))));

    var selections =
        List.of(
            new CvTehtavaSaveDto.Valinta(id1, Set.of(lapsi(childId1, osaaminen(osaaminenId1)))),
            new CvTehtavaSaveDto.Valinta(id3, Set.of(lapsi(childId3, osaaminen(osaaminenId3)))));

    var result =
        filterSelected(
            selections,
            items,
            GET_ID,
            GET_CHILDREN,
            GET_CHILD_ID,
            GET_OSAAMISET,
            WITH_FILTERED_OSAAMISET,
            WITH_FILTERED);

    assertThat(result).extracting(Parent::id).containsExactlyInAnyOrder(id1, id3);
  }

  @Test
  void shouldFilterChildren() {
    var parentId = UUID.randomUUID();
    var childId1 = UUID.randomUUID();
    var childId2 = UUID.randomUUID();
    var childId3 = UUID.randomUUID();
    var osaaminenId1 = UUID.randomUUID();
    var osaaminenId2 = UUID.randomUUID();
    var osaaminenId3 = UUID.randomUUID();

    var items =
        List.of(
            new Parent(
                parentId,
                "A",
                Set.of(
                    new Child(childId1, "c1", Set.of(osaaminen(osaaminenId1))),
                    new Child(childId2, "c2", Set.of(osaaminen(osaaminenId2))),
                    new Child(childId3, "c3", Set.of(osaaminen(osaaminenId3))))));

    var selections =
        List.of(
            new CvTehtavaSaveDto.Valinta(
                parentId,
                Set.of(
                    lapsi(childId1, osaaminen(osaaminenId1)),
                    lapsi(childId3, osaaminen(osaaminenId3)))));

    var result =
        filterSelected(
            selections,
            items,
            GET_ID,
            GET_CHILDREN,
            GET_CHILD_ID,
            GET_OSAAMISET,
            WITH_FILTERED_OSAAMISET,
            WITH_FILTERED);

    assertThat(result).hasSize(1);
    var parent = result.iterator().next();
    assertThat(parent.id()).isEqualTo(parentId);
    assertThat(parent.children())
        .extracting(Child::id)
        .containsExactlyInAnyOrder(childId1, childId3);
  }

  @Test
  void shouldFilterOsaamisetToSelectedKnownValues() {
    var parentId = UUID.randomUUID();
    var childId = UUID.randomUUID();
    var selectedId = UUID.randomUUID();
    var unselectedId = UUID.randomUUID();
    var unknownId = UUID.randomUUID();
    var items =
        List.of(
            new Parent(
                parentId,
                "A",
                Set.of(
                    new Child(
                        childId, "c1", Set.of(osaaminen(selectedId), osaaminen(unselectedId))))));
    var selections =
        List.of(
            new CvTehtavaSaveDto.Valinta(
                parentId, Set.of(lapsi(childId, osaaminen(selectedId), osaaminen(unknownId)))));

    var result =
        filterSelected(
            selections,
            items,
            GET_ID,
            GET_CHILDREN,
            GET_CHILD_ID,
            GET_OSAAMISET,
            WITH_FILTERED_OSAAMISET,
            WITH_FILTERED);

    assertThat(result)
        .singleElement()
        .satisfies(
            parent ->
                assertThat(parent.children())
                    .singleElement()
                    .satisfies(
                        child ->
                            assertThat(child.osaamiset()).containsExactly(osaaminen(selectedId))));
  }

  @Test
  void shouldReturnEmptySetWhenNothingMatches() {
    var items =
        List.of(
            new Parent(
                UUID.randomUUID(),
                "A",
                Set.of(new Child(UUID.randomUUID(), "c1", Set.of(osaaminen(UUID.randomUUID()))))));

    var selections = List.of(new CvTehtavaSaveDto.Valinta(UUID.randomUUID(), null));

    var result =
        filterSelected(
            selections,
            items,
            GET_ID,
            GET_CHILDREN,
            GET_CHILD_ID,
            GET_OSAAMISET,
            WITH_FILTERED_OSAAMISET,
            WITH_FILTERED);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptySetForNullSelections() {
    var items =
        List.of(
            new Parent(
                UUID.randomUUID(),
                "A",
                Set.of(new Child(UUID.randomUUID(), "c1", Set.of(osaaminen(UUID.randomUUID()))))));

    var result =
        filterSelected(
            null,
            items,
            GET_ID,
            GET_CHILDREN,
            GET_CHILD_ID,
            GET_OSAAMISET,
            WITH_FILTERED_OSAAMISET,
            WITH_FILTERED);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptySetForNullItems() {
    var selections = List.of(new CvTehtavaSaveDto.Valinta(UUID.randomUUID(), null));

    var result =
        filterSelected(
            selections,
            null,
            GET_ID,
            GET_CHILDREN,
            GET_CHILD_ID,
            GET_OSAAMISET,
            WITH_FILTERED_OSAAMISET,
            WITH_FILTERED);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldDiscardItemWhenChildrenAreNull() {
    var parentId = UUID.randomUUID();
    var items = List.of(new Parent(parentId, "A", null));

    var selections =
        List.of(
            new CvTehtavaSaveDto.Valinta(
                parentId, Set.of(lapsi(UUID.randomUUID(), osaaminen(UUID.randomUUID())))));

    var result =
        filterSelected(
            selections,
            items,
            GET_ID,
            GET_CHILDREN,
            GET_CHILD_ID,
            GET_OSAAMISET,
            WITH_FILTERED_OSAAMISET,
            WITH_FILTERED);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldDiscardItemWhenLapsetIsNull() {
    var parentId = UUID.randomUUID();
    var childId = UUID.randomUUID();
    var items =
        List.of(
            new Parent(
                parentId,
                "A",
                Set.of(new Child(childId, "c1", Set.of(osaaminen(UUID.randomUUID()))))));

    var selections = List.of(new CvTehtavaSaveDto.Valinta(parentId, null));

    var result =
        filterSelected(
            selections,
            items,
            GET_ID,
            GET_CHILDREN,
            GET_CHILD_ID,
            GET_OSAAMISET,
            WITH_FILTERED_OSAAMISET,
            WITH_FILTERED);

    assertThat(result).isEmpty();
  }
}
