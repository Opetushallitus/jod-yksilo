/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.logging;

import java.util.Collections;
import java.util.Iterator;
import org.slf4j.Marker;

public interface ImmutableMarker extends Marker {

  @Override
  default void add(Marker reference) {
    throw new UnsupportedOperationException("ImmutableMarker does not support add()");
  }

  @Override
  default boolean remove(Marker reference) {
    return false;
  }

  @Override
  @SuppressWarnings("deprecation")
  default boolean hasChildren() {
    return false;
  }

  @Override
  default boolean hasReferences() {
    return false;
  }

  @Override
  default Iterator<Marker> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  default boolean contains(Marker other) {
    return false;
  }

  @Override
  default boolean contains(String name) {
    return false;
  }
}
