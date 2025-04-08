/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.YksiloDto;
import fi.okm.jod.yksilo.dto.profiili.export.YksiloExportDto;
import fi.okm.jod.yksilo.service.profiili.YksiloService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/yksilo")
@Tag(name = "profiili/yksilo")
@RequiredArgsConstructor
public class YksiloController {
  private final YksiloService yksiloService;
  private final ObjectMapper objectMapper;

  @GetMapping
  public YksiloCsrfDto get(
      @AuthenticationPrincipal JodUser user, @Parameter(hidden = true) CsrfToken csrfToken) {
    return new YksiloCsrfDto(
        user.givenName(),
        user.familyName(),
        new CsrfTokenDto(
            csrfToken.getToken(), csrfToken.getHeaderName(), csrfToken.getParameterName()),
        yksiloService.get(user).tervetuloapolku());
  }

  @GetMapping("/vienti")
  public ResponseEntity<YksiloExportDto> export(@AuthenticationPrincipal JodUser user) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=profiili.json")
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .body(yksiloService.export(user));
  }

  @PutMapping
  public void update(@AuthenticationPrincipal JodUser user, @RequestBody @Valid YksiloDto dto) {
    yksiloService.update(user, dto);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(HttpServletRequest request, @AuthenticationPrincipal JodUser user)
      throws ServletException {
    yksiloService.delete(user);
    request.logout();
  }

  public record YksiloCsrfDto(
      String etunimi, String sukunimi, @NotNull CsrfTokenDto csrf, boolean tervetuloapolku) {}

  public record CsrfTokenDto(
      @NotNull String token, @NotNull String headerName, @NotNull String parameterName) {}
}
