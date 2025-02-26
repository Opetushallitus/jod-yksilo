/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

public class ResourceServerException extends KoskiServiceException {

  public ResourceServerException(String message) {
    super(message);
  }

  public ResourceServerException(String message, Throwable t) {
    super(message, t);
  }
}
