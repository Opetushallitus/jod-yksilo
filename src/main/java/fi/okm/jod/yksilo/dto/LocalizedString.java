/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import fi.okm.jod.yksilo.domain.Kieli;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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

  public LocalizedString(Map<@NotNull Kieli, @NotNull String> values) {
    this.values = Map.copyOf(values);
  }

  public <T> LocalizedString(Map<@NotNull Kieli, T> values, Function<T, String> mapper) {
    this.values =
        switch (values.size()) {
          case 0 -> Map.of();
          case 1 -> {
            var entry = values.entrySet().iterator().next();
            var value = entry.getValue() == null ? null : mapper.apply(entry.getValue());
            yield (value == null) ? Map.of() : Map.of(entry.getKey(), value);
          }
          default -> {
            // intentionally not using a stream to minimize creating of intermediate objects
            final var len = values.size();
            @SuppressWarnings("rawtypes")
            final var entries = new Map.Entry[len];
            var i = 0;
            for (var e : values.entrySet()) {
              if (e.getValue() != null && mapper.apply(e.getValue()) instanceof String s) {
                entries[i++] = Map.entry(e.getKey(), s);
              }
            }
            @SuppressWarnings("unchecked")
            final Map<Kieli, String> result =
                Map.ofEntries(i == len ? entries : Arrays.copyOf(entries, i));
            yield result;
          }
        };
  }

  public String get(Kieli kieli) {
    return values.get(kieli);
  }

  public Map<Kieli, String> asMap() {
    // due to the invariant, returning the map directly is safe
    return values;
  }

  @JsonCreator
  static LocalizedString fromJson(Map<@NotNull Kieli, String> values) {
    return new LocalizedString(values, s -> Normalizer.normalize(s.strip(), Normalizer.Form.NFKC));
  }
}
