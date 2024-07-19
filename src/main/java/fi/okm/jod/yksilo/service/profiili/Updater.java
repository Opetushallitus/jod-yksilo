/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

import fi.okm.jod.yksilo.domain.Identifiable;
import fi.okm.jod.yksilo.entity.OsaamisenLahde;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Updates a collection of entities (owned by a parent entity).
 *
 * @param <P> Parent entity that contains the collection of entities
 * @param <E> Entity
 * @param <D> Dto corresponding to the entity
 */
class Updater<P, E extends OsaamisenLahde, D extends Identifiable> {
  private final BiFunction<P, D, E> addEntity;
  private final BiConsumer<E, D> updateEntity;
  private final Consumer<E> deleteEntity;

  /**
   * Constructor.
   *
   * @param addEntity function to add a new entity
   * @param updateEntity function to update an existing entity
   * @param deleteEntity function to delete an existing entity
   */
  Updater(BiFunction<P, D, E> addEntity, BiConsumer<E, D> updateEntity, Consumer<E> deleteEntity) {
    this.addEntity = addEntity;
    this.updateEntity = updateEntity;
    this.deleteEntity = deleteEntity;
  }

  /**
   * Updates the collection of entities.
   *
   * @param parent Parent entity that "owns" then entities in the collection
   * @param existing Existing entities (mutable). Will be updated in place with possible new or
   *     removed entities.
   * @param dtos DTOs to update from
   * @return true if the update was successful, false if the dtos contained references to
   *     non-existing entities
   */
  boolean merge(P parent, Collection<E> existing, Collection<D> dtos) {

    var index =
        existing.stream()
            .collect(
                Collectors.toMap(OsaamisenLahde::getId, identity(), (a, b) -> a, HashMap::new));

    var newDtos = new ArrayList<D>(dtos.size());
    var updatedDtos = HashMap.<UUID, D>newHashMap(existing.size());

    for (var dto : dtos) {
      requireNonNull(dto);
      var id = dto.id();
      if (id == null) {
        newDtos.add(dto);
      } else if (index.containsKey(id)) {
        // duplicates by id silently ignored
        updatedDtos.put(id, dto);
      } else {
        return false;
      }
    }

    updatedDtos.forEach((id, d) -> updateEntity.accept(index.remove(id), d));

    index.values().forEach(deleteEntity);
    existing.removeAll(index.values());

    newDtos.forEach(dto -> existing.add(addEntity.apply(parent, dto)));

    return true;
  }
}
