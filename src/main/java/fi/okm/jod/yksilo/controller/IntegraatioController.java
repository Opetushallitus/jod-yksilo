/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.service.KoskiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integraatiot")
@RequiredArgsConstructor
@Tag(name = "integraatiot")
class IntegraatioController {
  private final KoskiService koskiService;

  // Define a set of allowed base URLs or patterns

  @GetMapping("/koski")
  @Operation(summary = "Try to import data from koski link")
  List<KoulutusDto> getKoskiData(@RequestParam("jakolinkki") URI jakolinkki) {

    var path = jakolinkki.getPath();
    if (path.startsWith("/koski/opinnot") && jakolinkki.getScheme().equals("https")) {
      return koskiService.getKoskiData(buildKoskiApiUri(jakolinkki));
    }
    throw new IllegalArgumentException("Invalid opintopolku uri");
  }

  private URI buildKoskiApiUri(URI uri) {
    var koskiApiUri =
        uri.getScheme()
            + "://"
            + uri.getHost()
            + uri.getPath().replaceFirst("koski/opinnot", "koski/api/opinnot");
    return URI.create(koskiApiUri);
  }
}
