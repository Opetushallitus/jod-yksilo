/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OsaamisetTunnistusEventHandler {

  @EventListener
  @Async
  public void handleOsaamisetTunnistusEvent(OsaamisetTunnistusEvent event) {
    try {
      log.debug("Osaamiset tunnistus event: {}", event);
      // Fetch entity from database
      // Make request to AI API
      // Update entity with response data.

    } catch (Exception e) {
      log.error("Error processing OsaamisetTunnistusEvent: {}", e.getMessage(), e);
    }
  }
}
