/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TestUtil {

  private TestUtil() {
    // Utility class.
  }

  public static String getContentFromFile(String filename, Class<?> clazz) {
    try (var inputStream = clazz.getResourceAsStream(filename)) {
      if (inputStream == null) {
        throw new RuntimeException(
            filename + " was NOT found in test resource folder: " + clazz.getPackageName());
      }
      try (var reader =
          new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        return reader.lines().collect(Collectors.joining("\n"));
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not read file: " + filename, e);
    }
  }
}
