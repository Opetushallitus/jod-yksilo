/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

@RequiredArgsConstructor
public class SivuDto<T> {
  private final Page<T> page;

  @JsonProperty("sisalto")
  public List<T> getContent() {
    return page.getContent();
  }

  @JsonProperty("maara")
  @Schema(example = "10")
  public long getTotalElements() {
    return page.getTotalElements();
  }

  @JsonProperty("sivuja")
  @Schema(example = "1")
  public int getTotalPages() {
    return page.getTotalPages();
  }
}
