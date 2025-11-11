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
import fi.okm.jod.yksilo.dto.profiili.JakolinkkiContentDto;
import fi.okm.jod.yksilo.service.profiili.JakolinkkiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cv")
@Tag(name = "cv")
@RequiredArgsConstructor
@Slf4j
@FeatureRequired(Feature.JAKOLINKKI)
public class PublicJakolinkkiController {

  private final JakolinkkiService jakolinkkiService;

  @GetMapping("/{ulkoinenJakolinkkiId}")
  public JakolinkkiContentDto getContent(@PathVariable UUID ulkoinenJakolinkkiId) {
    return jakolinkkiService.getContent(ulkoinenJakolinkkiId);
  }
}
