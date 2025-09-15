/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.validation;

import jakarta.validation.ConstraintValidator;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LanguageCodeValidator implements ConstraintValidator<LanguageCode, String> {

  private final Set<String> languageCodes;

  public LanguageCodeValidator(@Value("${jod.koodistot.kieli}") Set<String> languageCodes) {
    this.languageCodes = languageCodes;
  }

  @Override
  public boolean isValid(String value, jakarta.validation.ConstraintValidatorContext context) {
    return value == null || value.length() == 2 && languageCodes.contains(value);
  }
}
