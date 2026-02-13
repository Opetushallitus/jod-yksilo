/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.mapping;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.ObjectMapper;

@JsonTest
@Import({MappingConfig.class})
class MappingConfigTest {
  @Autowired ObjectMapper objectMapper;

  @Test
  void shouldEnforceStringLengthLimit() {
    var str = "x".repeat(MappingConfig.MAX_STRING_LEN * 2);
    var doc = "{\"field\":\"" + str + "\"}";
    assertThrows(StreamConstraintsException.class, () -> objectMapper.readTree(doc));
  }

  @Test
  void shouldEnforceDocumentSizeLimit() {
    var elem = "\"" + "x".repeat(1024) + "\"";
    int count =
        (int) MappingConfig.MAX_DOC_LEN / (elem.length() + 1)
            + 10 /* some margin as the limit is not exact */;
    var doc = "[" + elem + ("," + elem).repeat(count - 1) + "]";
    assertTrue(doc.length() > MappingConfig.MAX_DOC_LEN);
    assertThrows(StreamConstraintsException.class, () -> objectMapper.readTree(doc));
  }

  @Test
  void shouldAcceptValidDocument() {
    var elem = "\"" + "x".repeat(1024) + "\"";
    int count = (int) MappingConfig.MAX_DOC_LEN / (elem.length() + 1);
    var tmp = "[" + elem + ("," + elem).repeat(count - 1);
    tmp += ",\"" + "x".repeat((int) MappingConfig.MAX_DOC_LEN - tmp.length() - 4) + "\"]";
    var doc = tmp;

    assertEquals(MappingConfig.MAX_DOC_LEN, doc.length());
    assertDoesNotThrow(() -> objectMapper.readTree(doc));
  }
}
