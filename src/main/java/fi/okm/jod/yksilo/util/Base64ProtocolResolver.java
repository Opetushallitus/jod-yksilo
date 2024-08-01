/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.util;

import java.util.Base64;
import java.util.Base64.Decoder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;

public class Base64ProtocolResolver implements ProtocolResolver {

  private static final String BASE64_PREFIX = "base64:";

  // Using MIME decoder to ignore white space, makes it possible to use
  // multi-line strings in YAML config
  private static final Decoder decoder = Base64.getMimeDecoder();

  @Override
  public Resource resolve(@NonNull String location, @NonNull ResourceLoader resourceLoader) {
    if (location.startsWith(BASE64_PREFIX)) {
      return new ByteArrayResource(decoder.decode(location.substring(BASE64_PREFIX.length())));
    }
    return null;
  }
}
