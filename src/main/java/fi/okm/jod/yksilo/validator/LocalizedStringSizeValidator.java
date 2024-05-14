/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.validator;

import fi.okm.jod.yksilo.dto.LocalizedString;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Size;

public class LocalizedStringSizeValidator implements ConstraintValidator<Size, LocalizedString> {

  private int min;
  private int max;

  @Override
  public void initialize(Size constraintAnnotation) {
    this.min = constraintAnnotation.min();
    this.max = constraintAnnotation.max();
  }

  @Override
  public boolean isValid(LocalizedString value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return value.asMap().values().stream().allMatch(s -> s.length() >= min && s.length() <= max);
  }
}
