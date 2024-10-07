/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import fi.okm.jod.yksilo.dto.ArvoDto;
import fi.okm.jod.yksilo.dto.JakaumaDto;
import fi.okm.jod.yksilo.entity.Jakauma;
import fi.okm.jod.yksilo.entity.Jakauma.Arvo;
import java.util.List;

public final class JakaumaMapper {
  private JakaumaMapper() {}

  static JakaumaDto mapJakauma(Jakauma<?> jakauma) {
    return jakauma == null
        ? null
        : new JakaumaDto(jakauma.getMaara(), jakauma.getTyhjia(), mapArvot(jakauma.getArvot()));
  }

  private static List<ArvoDto> mapArvot(List<Arvo> arvot) {
    return arvot == null
        ? List.of()
        : arvot.stream().map(a -> new ArvoDto(a.arvo(), a.osuus())).toList();
  }
}
