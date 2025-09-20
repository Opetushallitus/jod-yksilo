/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.config.suomifi.Attribute;
import fi.okm.jod.yksilo.domain.FinnishPersonIdentifier;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import fi.okm.jod.yksilo.dto.profiili.YksiloDto;
import fi.okm.jod.yksilo.dto.profiili.export.YksiloExportDto;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import java.time.LocalDate;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class YksiloService {
  private final YksiloRepository yksilot;

  @Transactional(readOnly = true)
  public YksiloDto get(JodUser user) {
    var yksilo = getYksilo(user);

    var sukupuoli = yksilo.getSukupuoli();
    var syntymavuosi = yksilo.getSyntymavuosi();
    var kotikunta = yksilo.getKotikunta();
    var tunnisteTyyppi = (PersonIdentifierType) null;
    var email = yksilot.getEmail(user.getQualifiedPersonId()).orElse(null);

    if (!yksilo.getTervetuloapolku()) {
      if (getNationalPersonIdentifier(user) instanceof FinnishPersonIdentifier id) {
        tunnisteTyyppi = PersonIdentifierType.FIN;
        sukupuoli = id.getGender();
        syntymavuosi = id.getBirthYear();
        kotikunta = user.getAttribute(Attribute.KOTIKUNTA_KUNTANUMERO).orElse(null);
        email = user.getAttribute(Attribute.MAIL).orElse(null);
      } else if (user.getAttribute(Attribute.PERSON_IDENTIFIER).isPresent()) {
        tunnisteTyyppi = PersonIdentifierType.EIDAS;
        syntymavuosi =
            user.getAttribute(Attribute.DATE_OF_BIRTH)
                .map(it -> LocalDate.parse(it).getYear())
                .orElse(null);
      }
    }

    return new YksiloDto(
        tunnisteTyyppi,
        yksilo.getTervetuloapolku(),
        yksilo.getLupaLuovuttaaTiedotUlkopuoliselle(),
        yksilo.getLupaKayttaaTekoalynKoulutukseen(),
        syntymavuosi,
        sukupuoli,
        kotikunta,
        yksilo.getAidinkieli(),
        yksilo.getValittuKieli(),
        email);
  }

  public void update(JodUser user, YksiloDto dto) {
    var yksilo = getYksilo(user);
    yksilo.setTervetuloapolku(dto.tervetuloapolku());
    yksilo.setLupaLuovuttaaTiedotUlkopuoliselle(dto.lupaLuovuttaaTiedotUlkopuoliselle());
    yksilo.setLupaKayttaaTekoalynKoulutukseen(dto.lupaKayttaaTekoalynKoulutukseen());

    yksilo.setAidinkieli(dto.aidinkieli());
    yksilo.setValittuKieli(dto.valittuKieli());

    if (getNationalPersonIdentifier(user) instanceof FinnishPersonIdentifier id) {

      yksilo.setSyntymavuosi(validate(dto.syntymavuosi(), id.getBirthYear()));
      yksilo.setSukupuoli(validate(dto.sukupuoli(), id.getGender()));
      var kotikunta = user.getAttribute(Attribute.KOTIKUNTA_KUNTANUMERO).orElse(null);
      yksilo.setKotikunta(validate(dto.kotikunta(), kotikunta));

    } else if (user.getAttribute(Attribute.PERSON_IDENTIFIER).isPresent()) {

      var syntymavuosi =
          user.getAttribute(Attribute.DATE_OF_BIRTH)
              .map(it -> LocalDate.parse(it).getYear())
              .orElse(null);
      yksilo.setSyntymavuosi(validate(dto.syntymavuosi(), syntymavuosi));
      yksilo.setSukupuoli(dto.sukupuoli());
      if (dto.kotikunta() != null) {
        throw new ServiceValidationException("Kotikunta not supported eIDAS users");
      }
    }
    yksilot.updateEmail(user.getQualifiedPersonId(), dto.email());
    yksilot.save(yksilo);

    log.atInfo().addMarker(LogMarker.AUDIT).log("Updated user {} attributes", user.getId());
  }

  private static <T> T validate(T value, T expected) {
    if (value != null && !Objects.equals(value, expected)) {
      throw new ServiceValidationException("Changing attribute value is not allowed");
    }
    return value;
  }

  private static FinnishPersonIdentifier getNationalPersonIdentifier(JodUser user) {
    return user.getAttribute(Attribute.NATIONAL_IDENTIFICATION_NUMBER)
        .map(FinnishPersonIdentifier::of)
        .orElse(null);
  }

  public void delete(JodUser user) {
    yksilot.deleteById(user.getId());
    yksilot.removeId(user.getId());
    log.atInfo().addMarker(LogMarker.AUDIT).log("Deleted user {} profile", user.getId());
  }

  public YksiloExportDto export(JodUser user) {
    var exportDto =
        ExportMapper.mapYksilo(
            getYksilo(user), yksilot.getEmail(user.getQualifiedPersonId()).orElse(null));
    log.atInfo().addMarker(LogMarker.AUDIT).log("Exported user {} profile", user.getId());
    return exportDto;
  }

  private Yksilo getYksilo(JodUser user) {
    return yksilot
        .findById(user.getId())
        .orElseThrow(() -> new NotFoundException("Profiili does not exist"));
  }
}
