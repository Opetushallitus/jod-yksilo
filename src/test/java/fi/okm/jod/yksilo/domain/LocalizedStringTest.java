/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LocalizedStringTest {
  @Test
  void shouldCreateLocalizedString() {
    LocalizedString ls;

    ls = LocalizedString.of(Map.of(), Entity::s1);
    assertNull(ls);

    ls = LocalizedString.of(Map.of(Kieli.EN, new Entity(null, null)), Entity::s1);
    assertNull(ls);

    var data1 = new HashMap<Kieli, Entity>();
    data1.put(null, null);
    ls = LocalizedString.of(data1, Entity::s1);
    // special case, null value is ignored
    assertNull(ls);

    var data2 = new HashMap<Kieli, Entity>();
    data2.put(null, new Entity("s1", null));
    assertThrows(NullPointerException.class, () -> LocalizedString.of(data2, Entity::s1));

    ls = LocalizedString.of(Map.of(Kieli.EN, new Entity("s1", null)), Entity::s1);
    assertEquals(Map.of(Kieli.EN, "s1"), ls.asMap());
    assertEquals(ls(Kieli.EN, "s1"), ls);

    var data3 =
        Map.of(
            Kieli.FI,
            new Entity("s1", "s2"),
            Kieli.SV,
            new Entity(null, "s4"),
            Kieli.EN,
            new Entity("s5", null));

    ls = LocalizedString.of(data3, Entity::s1);
    assertNotNull(ls);
    final Map<Kieli, String> expected1 = Map.of(Kieli.FI, "s1", Kieli.EN, "s5");
    assertEquals(expected1, ls.asMap());

    ls = LocalizedString.of(data3, Entity::s2);
    var expected2 = Map.of(Kieli.FI, "s2", Kieli.SV, "s4");
    assertNotNull(ls);
    assertEquals(expected2, ls.asMap());
    assertNotEquals(new LocalizedString(expected1), ls);
    assertEquals(new LocalizedString(expected2), ls);
  }

  record Entity(String s1, String s2) {}
}
