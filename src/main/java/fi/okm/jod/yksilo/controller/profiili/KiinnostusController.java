/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.KiinnostuksetDto;
import fi.okm.jod.yksilo.service.profiili.KiinnostusService;
import fi.okm.jod.yksilo.validation.FreeText;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/kiinnostukset")
@Tag(name = "profiili/kiinnostukset")
@RequiredArgsConstructor
public class KiinnostusController {
  private final KiinnostusService kiinnostusService;

  @GetMapping("/osaamiset")
  public KiinnostuksetDto getOsaamiset(@AuthenticationPrincipal JodUser user) {
    return new KiinnostuksetDto(
        kiinnostusService.getOsaamiset(user), kiinnostusService.getVapaateksti(user));
  }

  @PutMapping("/osaamiset")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateOsaamiset(
      @AuthenticationPrincipal JodUser user, @RequestBody Set<@Valid URI> kiinnostukset) {
    kiinnostusService.updateOsaamiset(user, kiinnostukset);
  }

  @PutMapping("/vapaateksti")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateVapaateksti(
      @AuthenticationPrincipal JodUser user,
      @RequestBody @Size(max = 10000) @FreeText LocalizedString vapaateksti) {
    kiinnostusService.updateVapaateksti(user, vapaateksti);
  }
}
