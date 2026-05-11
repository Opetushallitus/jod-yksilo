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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FilterSelectedTest {

  // Simple test records to exercise the generic helper
  record Child(UUID id, String value) {}

  record Parent(UUID id, String name, Set<Child> children) {}

  private static final Function<Parent, UUID> GET_ID = Parent::id;
  private static final Function<Parent, Set<Child>> GET_CHILDREN = Parent::children;
  private static final Function<Child, UUID> GET_CHILD_ID = Child::id;
  private static final BiFunction<Parent, Set<Child>, Parent> WITH_FILTERED =
      (p, filtered) -> new Parent(p.id(), p.name(), filtered);

  @Test
  void shouldSelectMatchingItems() {
    var id1 = UUID.randomUUID();
    var id2 = UUID.randomUUID();
    var id3 = UUID.randomUUID();
    var childId1 = UUID.randomUUID();
    var childId3 = UUID.randomUUID();

    var items =
        List.of(
            new Parent(id1, "A", Set.of(new Child(childId1, "c1"))),
            new Parent(id2, "B", Set.of(new Child(UUID.randomUUID(), "c2"))),
            new Parent(id3, "C", Set.of(new Child(childId3, "c3"))));

    var selections =
        List.of(
            new CvTehtavaSaveDto.Valinta(id1, Set.of(childId1)),
            new CvTehtavaSaveDto.Valinta(id3, Set.of(childId3)));

    var result =
        filterSelected(selections, items, GET_ID, GET_CHILDREN, GET_CHILD_ID, WITH_FILTERED);

    assertThat(result).extracting(Parent::id).containsExactlyInAnyOrder(id1, id3);
  }

  @Test
  void shouldFilterChildren() {
    var parentId = UUID.randomUUID();
    var childId1 = UUID.randomUUID();
    var childId2 = UUID.randomUUID();
    var childId3 = UUID.randomUUID();

    var items =
        List.of(
            new Parent(
                parentId,
                "A",
                Set.of(
                    new Child(childId1, "c1"),
                    new Child(childId2, "c2"),
                    new Child(childId3, "c3"))));

    var selections = List.of(new CvTehtavaSaveDto.Valinta(parentId, Set.of(childId1, childId3)));

    var result =
        filterSelected(selections, items, GET_ID, GET_CHILDREN, GET_CHILD_ID, WITH_FILTERED);

    assertThat(result).hasSize(1);
    var parent = result.iterator().next();
    assertThat(parent.id()).isEqualTo(parentId);
    assertThat(parent.children())
        .extracting(Child::id)
        .containsExactlyInAnyOrder(childId1, childId3);
  }

  @Test
  void shouldReturnEmptySetWhenNothingMatches() {
    var items =
        List.of(new Parent(UUID.randomUUID(), "A", Set.of(new Child(UUID.randomUUID(), "c1"))));

    var selections = List.of(new CvTehtavaSaveDto.Valinta(UUID.randomUUID(), null));

    var result =
        filterSelected(selections, items, GET_ID, GET_CHILDREN, GET_CHILD_ID, WITH_FILTERED);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptySetForNullSelections() {
    var items =
        List.of(new Parent(UUID.randomUUID(), "A", Set.of(new Child(UUID.randomUUID(), "c1"))));

    var result = filterSelected(null, items, GET_ID, GET_CHILDREN, GET_CHILD_ID, WITH_FILTERED);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptySetForNullItems() {
    var selections = List.of(new CvTehtavaSaveDto.Valinta(UUID.randomUUID(), null));

    var result =
        filterSelected(selections, null, GET_ID, GET_CHILDREN, GET_CHILD_ID, WITH_FILTERED);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldDiscardItemWhenChildrenAreNull() {
    var parentId = UUID.randomUUID();
    var items = List.of(new Parent(parentId, "A", null));

    var selections = List.of(new CvTehtavaSaveDto.Valinta(parentId, Set.of(UUID.randomUUID())));

    var result =
        filterSelected(selections, items, GET_ID, GET_CHILDREN, GET_CHILD_ID, WITH_FILTERED);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldDiscardItemWhenLapsetIsNull() {
    var parentId = UUID.randomUUID();
    var childId = UUID.randomUUID();
    var items = List.of(new Parent(parentId, "A", Set.of(new Child(childId, "c1"))));

    var selections = List.of(new CvTehtavaSaveDto.Valinta(parentId, null));

    var result =
        filterSelected(selections, items, GET_ID, GET_CHILDREN, GET_CHILD_ID, WITH_FILTERED);

    assertThat(result).isEmpty();
  }
}
