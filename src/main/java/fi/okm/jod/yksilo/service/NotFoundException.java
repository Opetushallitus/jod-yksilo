/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

@SuppressWarnings("serial")
public class NotFoundException extends ServiceException {
  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotFoundException(Throwable cause) {
    super(cause);
  }
}
