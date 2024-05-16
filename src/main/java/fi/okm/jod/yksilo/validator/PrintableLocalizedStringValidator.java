/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.validator;

import fi.okm.jod.yksilo.domain.LocalizedString;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PrintableLocalizedStringValidator
    implements ConstraintValidator<PrintableString, LocalizedString> {

  private static final Pattern PAT =
      Pattern.compile("[\\w\\p{Punct}\\p{gc=S}\\x20]*", Pattern.UNICODE_CHARACTER_CLASS);

  @Override
  public boolean isValid(LocalizedString value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    for (var s : value.asMap().values()) {
      if (!PAT.matcher(s).matches()) {
        return false;
      }
    }
    return true;
  }
}
