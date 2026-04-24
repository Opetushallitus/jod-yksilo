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
import fi.okm.jod.yksilo.dto.profiili.YksiloDto;
import fi.okm.jod.yksilo.dto.profiili.export.YksiloExportDto;
import fi.okm.jod.yksilo.service.profiili.YksiloService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/yksilo")
@Tag(name = "profiili/yksilo")
@RequiredArgsConstructor
@Slf4j
public class YksiloController {
  private final YksiloService yksiloService;

  @GetMapping
  public YksiloCsrfDto get(
      @Parameter(hidden = true) CsrfToken csrfToken, Authentication authentication) {
    if (authentication.getPrincipal() instanceof JodUser user) {
      var yksilo = yksiloService.get(user);
      var capabilities = EnumSet.noneOf(Capability.class);
      capabilities.add(Capability.PROFILE);
      if (authentication.getAuthorities().stream()
          .anyMatch(a -> "ROLE_FULL_USER".equals(a.getAuthority()))) {
        capabilities.add(Capability.TMT);
        capabilities.add(Capability.KOSKI);
      }
      return new YksiloCsrfDto(
          user.givenName(),
          user.familyName(),
          yksilo.tervetuloapolku(),
          capabilities,
          new CsrfTokenDto(
              csrfToken.getToken(), csrfToken.getHeaderName(), csrfToken.getParameterName()));
    }
    throw new InsufficientAuthenticationException("User is not authenticated");
  }

  @GetMapping("/tiedot-ja-luvat")
  public YksiloDto getTiedotJaLuvat(@AuthenticationPrincipal JodUser user) {
    return yksiloService.get(user);
  }

  @PutMapping("/tiedot-ja-luvat")
  public void updateTiedotJaLuvat(
      @AuthenticationPrincipal JodUser user, @RequestBody @Valid YksiloDto dto) {
    yksiloService.update(user, dto);
  }

  @GetMapping("/vienti")
  public ResponseEntity<YksiloExportDto> export(@AuthenticationPrincipal JodUser user) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=profiili.json")
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .body(yksiloService.export(user));
  }

  public record YksiloCsrfDto(
      String etunimi,
      String sukunimi,
      boolean tervetuloapolku,
      @NotNull Set<Capability> toiminnot,
      @NotNull CsrfTokenDto csrf) {}

  public record CsrfTokenDto(
      @NotNull String token, @NotNull String headerName, @NotNull String parameterName) {}

  public enum Capability {
    PROFILE,
    TMT,
    KOSKI
  }
}
