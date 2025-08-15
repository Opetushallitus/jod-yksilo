/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.admin.dto;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Koulutusdata */
@Setter
public class FullKoulutusData {
  @Getter private String oid;
  private String koulutuskoodit;
  private String koulutusAlat;

  public List<String> getKoulutuskoodit() {
    // Remove brackets and quotes, then split
    return Arrays.stream(koulutuskoodit.replaceAll("[\\[\\]']", "").split(","))
        .map(String::trim)
        .toList();
  }

  public List<String> getKoulutusAlat() {
    // Remove brackets and quotes, then split
    return Arrays.stream(koulutusAlat.replaceAll("[\\[\\]']", "").split(","))
        .map(String::trim)
        .toList();
  }
}
