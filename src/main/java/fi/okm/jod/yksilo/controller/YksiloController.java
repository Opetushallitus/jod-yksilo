/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.http.MediaType.IMAGE_PNG;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.CsrfTokenDto;
import fi.okm.jod.yksilo.dto.YksiloCsrfDto;
import fi.okm.jod.yksilo.service.YksiloService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yksilo")
@Tag(name = "yksilo", description = "Yksilön toiminnot")
public class YksiloController {
  private final YksiloService yksiloService;
  private static final List<MediaType> ALLOWED_IMAGE_CONTENT_TYPES = List.of(IMAGE_PNG, IMAGE_JPEG);

  public YksiloController(YksiloService kayttajaService) {
    this.yksiloService = kayttajaService;
  }

  @GetMapping
  public YksiloCsrfDto getYksilo(
      @AuthenticationPrincipal JodUser user, @Parameter(hidden = true) CsrfToken csrfToken) {
    return new YksiloCsrfDto(
        yksiloService.findYksilo(user),
        user.givenName(),
        user.familyName(),
        new CsrfTokenDto(
            csrfToken.getToken(), csrfToken.getHeaderName(), csrfToken.getParameterName()));
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteYksilo(HttpServletRequest request, @AuthenticationPrincipal JodUser user)
      throws ServletException {
    yksiloService.deleteYksilo(user);
    request.logout();
  }
}
