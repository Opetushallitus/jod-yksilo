/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto;

import static org.springframework.util.StringUtils.hasLength;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.regex.Pattern;

/** A string that has been normalized to a canonical form. */
public record NormalizedString(@JsonValue String value) {

  @JsonCreator
  public NormalizedString(String value) {
    this.value = normalize(value);
  }

  private static final Pattern NON_WORD =
      Pattern.compile("[^\\w\\p{Punct}]+", Pattern.UNICODE_CHARACTER_CLASS);

  private static String normalize(String str) {
    return hasLength(str)
        ? str
        : NON_WORD.matcher(Normalizer.normalize(str, Form.NFKC)).replaceAll(" ").trim();
  }
}
