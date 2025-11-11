/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import fi.okm.jod.yksilo.config.feature.Feature;
import fi.okm.jod.yksilo.config.feature.FeatureRequired;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.JakolinkkiUpdateDto;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.service.profiili.JakolinkkiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/jakolinkki")
@Tag(name = "profiili/jakolinkki")
@RequiredArgsConstructor
@Slf4j
@FeatureRequired(Feature.JAKOLINKKI)
public class JakolinkkiController {

  private final JakolinkkiService jakolinkkiService;

  @PostMapping
  public void create(
      @AuthenticationPrincipal JodUser user,
      @Validated({Add.class}) @RequestBody JakolinkkiUpdateDto dto) {
    jakolinkkiService.create(user, dto);
  }

  @PatchMapping
  public void update(
      @AuthenticationPrincipal JodUser user, @Valid @RequestBody JakolinkkiUpdateDto dto) {
    jakolinkkiService.update(user, dto);
  }

  @GetMapping
  public List<JakolinkkiUpdateDto> list(@AuthenticationPrincipal JodUser user) {
    return jakolinkkiService.list(user);
  }

  @GetMapping("/{id}")
  public JakolinkkiUpdateDto get(@AuthenticationPrincipal JodUser user, @PathVariable UUID id) {
    return jakolinkkiService.get(user, id);
  }

  @DeleteMapping("/{id}")
  public void delete(@AuthenticationPrincipal JodUser user, @PathVariable UUID id) {
    jakolinkkiService.delete(user, id);
  }
}
