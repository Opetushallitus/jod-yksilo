/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import jakarta.persistence.Embeddable;
import java.util.List;

public interface Jakauma<T extends Enum<T>> {

  T getTyyppi();

  int getMaara();

  int getTyhjia();

  List<Arvo> getArvot();

  @Embeddable
  record Arvo(String arvo, double osuus) {}
}
