/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.event;

import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import jakarta.servlet.UnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class OsaamisetTunnistusEventHandler {

  private final KoulutusRepository koulutusRepository;

  @EventListener
  @Async
  public void handleOsaamisetTunnistusEvent(OsaamisetTunnistusEvent event) {
    try {
      log.debug("Osaamiset tunnistus event: {}", event);
      // Make request to AI API
      // Update entity with response data.
      Thread.sleep(5_000);
      throw new UnavailableException("Not yet implemented: Osaamiset tunnistus");

    } catch (Exception e) {
      log.error("Error processing OsaamisetTunnistusEvent: {}", e.getMessage(), e);
      updateOsaamisetTunnistusStatus(event, OsaamisenTunnistusStatus.FAIL);
    }
  }

  private void updateOsaamisetTunnistusStatus(
      OsaamisetTunnistusEvent event, OsaamisenTunnistusStatus newStatus) {
    for (var koulutusKokonaisuus : event.koulutusKokonaisuusList()) {
      var koulutukset = koulutusKokonaisuus.getKoulutukset();
      for (var koulutus : koulutukset) {
        koulutus.setOsaamisenTunnistusStatus(newStatus);
      }
      koulutusRepository.saveAllAndFlush(koulutukset);
    }
  }
}
