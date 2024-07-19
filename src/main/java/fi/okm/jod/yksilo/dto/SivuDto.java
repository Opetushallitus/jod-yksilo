/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.data.domain.Page;

public record SivuDto<T>(
    @NotNull List<T> sisalto,
    @NotNull @Schema(example = "30") long maara,
    @NotNull @Schema(example = "3") int sivuja) {

  public SivuDto(Page<T> page) {
    this(page.getContent(), page.getTotalElements(), page.getTotalPages());
  }
}
