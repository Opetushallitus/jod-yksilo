/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import lombok.EqualsAndHashCode;

@Schema(
    name = "LokalisoituTeksti",
    propertyNames = Kieli.class,
    additionalPropertiesSchema = String.class,
    example = "{\"fi\":\"suomeksi\", \"sv\":\"på svenska\"}")
@EqualsAndHashCode
public final class LocalizedString {
  // invariant: immutable map of the localized values, no null keys or values
  private final Map<Kieli, String> values;

  public LocalizedString(Map<Kieli, String> values) {
    var immutableValues = Map.copyOf(requireNonNull(values));
    if (immutableValues.isEmpty()) {
      throw new IllegalArgumentException("values must not be empty");
    }
    this.values = immutableValues;
  }

  @Schema(hidden = true)
  public String get(Kieli kieli) {
    return values.get(requireNonNull(kieli));
  }

  public Map<Kieli, String> asMap() {
    // due to the invariant, returning the map directly is safe
    return values;
  }

  /**
   * Creates a new instance of LocalizedString from a Map, normalizing the string values. Intended
   * to be used when deserializing JSON.
   */
  @JsonCreator
  public static LocalizedString fromJsonNormalized(Map<Kieli, String> values) {
    return LocalizedString.of(values, s -> Normalizer.normalize(s.strip(), Normalizer.Form.NFKC));
  }

  public static <T> LocalizedString of(Map<Kieli, T> values, Function<T, String> mapper) {
    requireNonNull(mapper);
    if (values == null) {
      return null;
    }
    Map<Kieli, String> map =
        switch (values.size()) {
          case 0 -> Map.of();
          case 1 -> {
            var entry = values.entrySet().iterator().next();
            var value = entry.getValue() == null ? null : mapper.apply(entry.getValue());
            yield (value == null) ? Map.of() : Map.of(entry.getKey(), value);
          }
          default -> toMap(values, mapper);
        };
    return map.isEmpty() ? null : new LocalizedString(map);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static <T> Map<Kieli, String> toMap(Map<Kieli, T> values, Function<T, String> mapper) {
    // intentionally not using a stream to minimize creating of intermediate objects
    final var len = values.size();
    final var entries = new Map.Entry[len];
    var i = 0;
    for (var e : values.entrySet()) {
      if (e.getValue() != null && mapper.apply(e.getValue()) instanceof String s) {
        entries[i++] = Map.entry(e.getKey(), s);
      }
    }
    return Map.ofEntries(i == len ? entries : Arrays.copyOf(entries, i));
  }
}
