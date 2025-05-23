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
public class ServiceConflictException extends ServiceException {
  public ServiceConflictException(String message) {
    super(message);
  }

  public ServiceConflictException(String message, Throwable cause) {
    super(message, cause);
  }

  public ServiceConflictException(Throwable cause) {
    super(cause);
  }
}
