/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.dto.profiili.JakolinkkiContentDto;
import fi.okm.jod.yksilo.dto.profiili.JakolinkkiUpdateDto;
import fi.okm.jod.yksilo.dto.profiili.KiinnostuksetDto;
import fi.okm.jod.yksilo.dto.profiili.MuuOsaaminenDto;
import fi.okm.jod.yksilo.dto.profiili.SuosikkiDto;
import fi.okm.jod.yksilo.entity.Jakolinkki;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Tavoite;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen_;
import fi.okm.jod.yksilo.entity.YksilonSuosikki;
import fi.okm.jod.yksilo.repository.JakolinkkiRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.repository.YksilonOsaaminenRepository;
import fi.okm.jod.yksilo.repository.projection.JakolinkkiSettings;
import fi.okm.jod.yksilo.service.NotFoundException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class JakolinkkiService {
  private final JakolinkkiRepository jakolinkkiRepository;
  private final YksiloRepository yksiloRepository;
  private final YksilonOsaaminenRepository yksilonOsaaminenRepository;
  private final YksiloService yksiloService;

  private static final String JAKOLINKKI_NOT_FOUND = "Jakolinkki does not exist";

  public void create(JodUser jodUser, JakolinkkiUpdateDto jakolinkkiDto) {

    var yksilo = yksiloService.getYksilo(jodUser);

    var jakolinkkiId =
        jakolinkkiRepository.createJakolinkki(
            jodUser.getQualifiedPersonId(),
            jakolinkkiDto.voimassaAsti(),
            jakolinkkiDto.nimiJaettu(),
            jakolinkkiDto.emailJaettu());

    var jakolinkki = new Jakolinkki();
    jakolinkki.setId(jakolinkkiId);
    jakolinkki.setYksilo(yksilo);
    mapToJakolinkki(jakolinkki, jakolinkkiDto, yksilo);
    jakolinkkiRepository.save(jakolinkki);

    logJakolinkkiActionAudit("created", jodUser.getQualifiedPersonId(), jakolinkki);
  }

  public void update(JodUser jodUser, JakolinkkiUpdateDto jakolinkkiDto) {

    var jakolinkki =
        jakolinkkiRepository
            .findById(jakolinkkiDto.id())
            .orElseThrow(() -> new NotFoundException(JAKOLINKKI_NOT_FOUND));

    var yksilo = yksiloService.getYksilo(jodUser);

    if (!jakolinkki.getYksilo().getId().equals(yksilo.getId())) {
      log.atWarn()
          .addMarker(LogMarker.AUDIT)
          .log(
              "User {} attempted to update jakolinkki {} which does not belong to them",
              jodUser.getId(),
              jakolinkkiDto.id());
      throw new NotFoundException(JAKOLINKKI_NOT_FOUND);
    }

    jakolinkkiRepository.updateJakolinkki(
        jodUser.getQualifiedPersonId(),
        jakolinkkiDto.id(),
        jakolinkkiDto.voimassaAsti(),
        jakolinkkiDto.nimiJaettu(),
        jakolinkkiDto.emailJaettu());

    jakolinkkiRepository.save(mapToJakolinkki(jakolinkki, jakolinkkiDto, yksilo));

    logJakolinkkiActionAudit("updated", jodUser.getQualifiedPersonId(), jakolinkki);
  }

  @Transactional(readOnly = true)
  public List<JakolinkkiUpdateDto> list(JodUser jodUser) {
    var yksilo = yksiloService.getYksilo(jodUser);

    return jakolinkkiRepository.getJakolinkit(jodUser.getQualifiedPersonId()).stream()
        .map(jakolinkkiSettings -> mapToJakolinkkiUpdateDto(jakolinkkiSettings, yksilo))
        .toList();
  }

  @Transactional(readOnly = true)
  public JakolinkkiUpdateDto get(JodUser jodUser, UUID jakolinkkiId) {
    var jakolinkkiSettings =
        jakolinkkiRepository
            .getJakolinkki(jodUser.getQualifiedPersonId(), jakolinkkiId)
            .orElseThrow(
                () -> {
                  log.atInfo()
                      .addMarker(LogMarker.AUDIT)
                      .log(
                          "User {} Jakolinkki not found with ulkoinenId {}",
                          jodUser.getId(),
                          jakolinkkiId);
                  return new NotFoundException(JAKOLINKKI_NOT_FOUND);
                });

    var yksilo = yksiloService.getYksilo(jodUser);

    return mapToJakolinkkiUpdateDto(jakolinkkiSettings, yksilo);
  }

  @Transactional(readOnly = true)
  public JakolinkkiContentDto getContent(UUID ulkoinenJakolinkkiId) {

    var jakolinkkiDetails =
        jakolinkkiRepository
            .getJakolinkkiByUlkoinenId(ulkoinenJakolinkkiId)
            .orElseThrow(
                () -> {
                  log.atInfo()
                      .addMarker(LogMarker.AUDIT)
                      .log(
                          "Invalid jakolinkki access attempt with ulkoinenId {}. Jakolinkki not found or is expired.",
                          ulkoinenJakolinkkiId);
                  return new NotFoundException(JAKOLINKKI_NOT_FOUND);
                });

    var jakolinkki =
        jakolinkkiRepository
            .findById(jakolinkkiDetails.getJakolinkkiId())
            .orElseThrow(
                () -> {
                  log.atInfo()
                      .addMarker(LogMarker.AUDIT)
                      .log("Jakolinkki not found for id {}", jakolinkkiDetails.getJakolinkkiId());
                  return new NotFoundException(JAKOLINKKI_NOT_FOUND);
                });

    var yksilo = jakolinkki.getYksilo();

    var osaaminenSort =
        Sort.by(
            YksilonOsaaminen_.LAHDE,
            YksilonOsaaminen_.TOIMENKUVA,
            YksilonOsaaminen_.KOULUTUS,
            YksilonOsaaminen_.ID);

    var jaetutTyopaikatIds =
        jakolinkki.getTyopaikat().stream().map(Tyopaikka::getId).collect(Collectors.toSet());
    var jaetutKoulutuksetIds =
        jakolinkki.getKoulutukset().stream()
            .map(KoulutusKokonaisuus::getId)
            .collect(Collectors.toSet());
    var jaetutToiminnotIds =
        jakolinkki.getToiminnot().stream().map(Toiminto::getId).collect(Collectors.toSet());
    var jaetutTavoitteetIds =
        jakolinkki.getTavoitteet().stream().map(Tavoite::getId).collect(Collectors.toSet());

    log.atInfo()
        .addMarker(LogMarker.AUDIT)
        .log(
            "Somebody is viewing user {} profile via jakolinkki {}",
            yksilo.getId(),
            jakolinkkiDetails.getJakolinkkiId());

    return new JakolinkkiContentDto(
        jakolinkkiDetails.getVoimassaAsti(),
        getSharedValue(jakolinkkiDetails.getNimiJaettu(), jakolinkkiDetails.getEtunimi()),
        getSharedValue(jakolinkkiDetails.getNimiJaettu(), jakolinkkiDetails.getSukunimi()),
        getSharedValue(jakolinkkiDetails.getEmailJaettu(), jakolinkkiDetails.getEmail()),
        getSharedValue(jakolinkki.isKotikuntaJaettu(), yksilo.getKotikunta()),
        getSharedValue(jakolinkki.isSyntymavuosiJaettu(), yksilo.getSyntymavuosi()),
        yksilo.getTyopaikat().stream()
            .filter(t -> jaetutTyopaikatIds.contains(t.getId()))
            .map(Mapper::mapTyopaikka)
            .collect(Collectors.toSet()),
        yksilo.getKoulutusKokonaisuudet().stream()
            .filter(k -> jaetutKoulutuksetIds.contains(k.getId()))
            .map(Mapper::mapKoulutusKokonaisuus)
            .collect(Collectors.toSet()),
        yksilo.getToiminnot().stream()
            .filter(t -> jaetutToiminnotIds.contains(t.getId()))
            .map(Mapper::mapToiminto)
            .collect(Collectors.toSet()),
        jakolinkki.isMuuOsaaminenJaettu() ? mapMuuOsaaminen(yksilo, osaaminenSort) : null,
        mapSuosikit(
            yksilo.getSuosikit(),
            jakolinkki.isKoulutusmahdollisuusSuosikitJaettu(),
            jakolinkki.isTyomahdollisuusSuosikitJaettu()),
        jakolinkki.isKiinnostuksetJaettu() ? mapKiinnostukset(yksilo) : null,
        yksilo.getTavoitteet().stream()
            .filter(t -> jaetutTavoitteetIds.contains(t.getId()))
            .map(Mapper::mapTavoite)
            .collect(Collectors.toSet()));
  }

  public void delete(JodUser user, UUID id) {
    var yksilo = yksiloService.getYksilo(user);
    var jakolinkki =
        jakolinkkiRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.atInfo()
                      .addMarker(LogMarker.AUDIT)
                      .log(
                          "User {} attempted to delete jakolinkki {} which does not exist",
                          user.getId(),
                          id);
                  return new NotFoundException(JAKOLINKKI_NOT_FOUND);
                });
    if (!jakolinkki.getYksilo().getId().equals(yksilo.getId())) {
      log.atWarn()
          .addMarker(LogMarker.AUDIT)
          .log(
              "User {} attempted to delete jakolinkki {} which does not belong to them",
              user.getId(),
              id);
      throw new NotFoundException(JAKOLINKKI_NOT_FOUND);
    }
    jakolinkkiRepository.deleteById(id);
    jakolinkkiRepository.deleteJakolinkki(user.getQualifiedPersonId(), id);
    log.atInfo().addMarker(LogMarker.AUDIT).log("User {} deleted jakolinkki {}", user.getId(), id);
  }

  private Jakolinkki mapToJakolinkki(
      Jakolinkki jakolinkki, JakolinkkiUpdateDto dto, Yksilo yksilo) {

    jakolinkki.setNimi(dto.nimi());
    jakolinkki.setMuistiinpano(dto.muistiinpano());
    jakolinkki.setKotikuntaJaettu(dto.kotikuntaJaettu());
    jakolinkki.setSyntymavuosiJaettu(dto.syntymavuosiJaettu());
    jakolinkki.setMuuOsaaminenJaettu(dto.muuOsaaminenJaettu());
    jakolinkki.setKiinnostuksetJaettu(dto.kiinnostuksetJaettu());
    jakolinkki.setTyopaikat(
        yksilo.getTyopaikat().stream()
            .filter(t -> dto.jaetutTyopaikat().contains(t.getId()))
            .collect(Collectors.toSet()));
    jakolinkki.setKoulutukset(
        yksilo.getKoulutusKokonaisuudet().stream()
            .filter(k -> dto.jaetutKoulutukset().contains(k.getId()))
            .collect(Collectors.toSet()));
    jakolinkki.setToiminnot(
        yksilo.getToiminnot().stream()
            .filter(to -> dto.jaetutToiminnot().contains(to.getId()))
            .collect(Collectors.toSet()));
    jakolinkki.setKoulutusmahdollisuusSuosikitJaettu(
        dto.jaetutSuosikit().contains(SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS));
    jakolinkki.setTyomahdollisuusSuosikitJaettu(
        dto.jaetutSuosikit().contains(SuosikkiTyyppi.TYOMAHDOLLISUUS));
    jakolinkki.setTavoitteet(
        yksilo.getTavoitteet().stream()
            .filter(t -> dto.jaetutTavoitteet().contains(t.getId()))
            .collect(Collectors.toSet()));
    return jakolinkki;
  }

  private <T> T getSharedValue(boolean isShared, T value) {
    return isShared ? value : null;
  }

  private MuuOsaaminenDto mapMuuOsaaminen(Yksilo yksilo, Sort sort) {
    var osaamiset =
        yksilonOsaaminenRepository
            .findAllBy(yksilo.getId(), OsaamisenLahdeTyyppi.MUU_OSAAMINEN, sort)
            .stream()
            .map(yo -> yo.getOsaaminen().getUri())
            .collect(Collectors.toSet());
    return new MuuOsaaminenDto(osaamiset, yksilo.getMuuOsaaminenVapaateksti());
  }

  private KiinnostuksetDto mapKiinnostukset(Yksilo yksilo) {
    var kiinnostukset =
        Stream.concat(
                yksiloRepository.findAmmattiKiinnostukset(yksilo).stream(),
                yksiloRepository.findOsaamisKiinnostukset(yksilo).stream())
            .map(URI::create)
            .collect(Collectors.toSet());
    return new KiinnostuksetDto(kiinnostukset, yksilo.getOsaamisKiinnostuksetVapaateksti());
  }

  private Set<SuosikkiDto> mapSuosikit(
      Set<YksilonSuosikki> suosikit,
      boolean koulutusmahdollisuusSuosikitJaettu,
      boolean tyomahdollisuusSuosikitJaettu) {
    return suosikit.stream()
        .filter(
            s ->
                (s.getTyyppi() == SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS
                        && koulutusmahdollisuusSuosikitJaettu)
                    || (s.getTyyppi() == SuosikkiTyyppi.TYOMAHDOLLISUUS
                        && tyomahdollisuusSuosikitJaettu))
        .map(Mapper::mapYksilonSuosikki)
        .collect(Collectors.toSet());
  }

  private void logJakolinkkiActionAudit(String action, String userId, Jakolinkki jakolinkki) {
    var jakolinkkiSettings =
        jakolinkkiRepository.getJakolinkki(userId, jakolinkki.getId()).orElse(null);

    log.atInfo()
        .addMarker(LogMarker.AUDIT)
        .log(
            "User {} {} jakolinkki {} (valid until: {}). Shared: nimi={}, email={}, kotikunta={}, syntymavuosi={}, "
                + "tyopaikat={}, koulutukset={}, toiminnot={}, muuOsaaminen={}, kiinnostukset={}, "
                + "koulutusmahdollisuusSuosikit={}, tyomahdollisuusSuosikit={}, tavoitteet={}",
            jakolinkki.getYksilo().getId(),
            action,
            jakolinkki.getId(),
            jakolinkkiSettings != null ? jakolinkkiSettings.getVoimassaAsti() : null,
            jakolinkkiSettings != null && jakolinkkiSettings.getNimiJaettu(),
            jakolinkkiSettings != null && jakolinkkiSettings.getEmailJaettu(),
            jakolinkki.isKotikuntaJaettu(),
            jakolinkki.isSyntymavuosiJaettu(),
            jakolinkki.getTyopaikat().size(),
            jakolinkki.getKoulutukset().size(),
            jakolinkki.getToiminnot().size(),
            jakolinkki.isMuuOsaaminenJaettu(),
            jakolinkki.isKiinnostuksetJaettu(),
            jakolinkki.isKoulutusmahdollisuusSuosikitJaettu(),
            jakolinkki.isTyomahdollisuusSuosikitJaettu(),
            jakolinkki.getTavoitteet().size());
  }

  private JakolinkkiUpdateDto mapToJakolinkkiUpdateDto(
      JakolinkkiSettings jakolinkkiSettings, Yksilo yksilo) {
    var jakolinkki =
        jakolinkkiRepository
            .findById(jakolinkkiSettings.getJakolinkkiId())
            .orElseThrow(() -> new NotFoundException(JAKOLINKKI_NOT_FOUND));

    if (!jakolinkki.getYksilo().getId().equals(yksilo.getId())) {
      throw new NotFoundException(JAKOLINKKI_NOT_FOUND);
    }

    return new JakolinkkiUpdateDto(
        jakolinkki.getId(),
        jakolinkkiSettings.getUlkoinenId(),
        jakolinkki.getNimi(),
        jakolinkkiSettings.getVoimassaAsti(),
        jakolinkki.getMuistiinpano(),
        jakolinkkiSettings.getNimiJaettu(),
        jakolinkkiSettings.getEmailJaettu(),
        jakolinkki.isKotikuntaJaettu(),
        jakolinkki.isSyntymavuosiJaettu(),
        jakolinkki.isMuuOsaaminenJaettu(),
        jakolinkki.isKiinnostuksetJaettu(),
        jakolinkki.getTyopaikat().stream().map(Tyopaikka::getId).collect(Collectors.toSet()),
        jakolinkki.getKoulutukset().stream()
            .map(KoulutusKokonaisuus::getId)
            .collect(Collectors.toSet()),
        jakolinkki.getToiminnot().stream().map(Toiminto::getId).collect(Collectors.toSet()),
        Arrays.stream(SuosikkiTyyppi.values())
            .filter(
                st ->
                    (st == SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS
                            && jakolinkki.isKoulutusmahdollisuusSuosikitJaettu())
                        || (st == SuosikkiTyyppi.TYOMAHDOLLISUUS
                            && jakolinkki.isTyomahdollisuusSuosikitJaettu()))
            .collect(Collectors.toSet()),
        jakolinkki.getTavoitteet().stream().map(Tavoite::getId).collect(Collectors.toSet()));
  }
}
