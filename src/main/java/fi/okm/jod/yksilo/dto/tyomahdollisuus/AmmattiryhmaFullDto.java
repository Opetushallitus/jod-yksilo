package fi.okm.jod.yksilo.dto.tyomahdollisuus;

import java.net.URI;

/**
 * Data of the ammattiryhma
 */
public record AmmattiryhmaFullDto(
    URI ammattiryhma,
    Integer mediaaniPalkka,
    Integer ylinDesiiliPalkka,
    Integer alinDesiiliPalkka,
    String kohtaanto
) {
}
