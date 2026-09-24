/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;

public record CvTehtavaSaveDto(
    List<@Valid @NotNull Valinta> koulutuskokonaisuudet,
    List<@Valid @NotNull Valinta> tyopaikat,
    List<@Valid @NotNull Valinta> teemat) {

  @Schema(name = "CvValinta")
  public record Valinta(
      @NotNull UUID id,
      @NotEmpty @JsonDeserialize(contentUsing = LapsiDeserializer.class)
          Set<@Valid @NotNull Lapsi> lapset) {}

  @Schema(name = "CvLapsi")
  public record Lapsi(@NotNull UUID id, Set<@NotNull URI> osaamiset) {}

  // for backwards compatibility (to be removed after upgrade)
  public static class LapsiDeserializer extends StdDeserializer<Lapsi> {

    public LapsiDeserializer() {
      super(Lapsi.class);
    }

    @Override
    public Lapsi deserialize(JsonParser parser, DeserializationContext context)
        throws JacksonException {
      if (parser.hasToken(JsonToken.VALUE_STRING)) {
        return new Lapsi(context.readValue(parser, UUID.class), Set.of());
      }
      if (parser.hasToken(JsonToken.START_OBJECT)) {
        return context.readValue(parser, Lapsi.class);
      }
      return (Lapsi) context.handleUnexpectedToken(Lapsi.class, parser);
    }
  }
}
