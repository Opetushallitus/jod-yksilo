/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import fi.okm.jod.yksilo.config.suomifi.JodSaml2Principal;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.YksiloDto;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class YksiloService {
  private final YksiloRepository yksilot;

  public Yksilo get(JodUser user) {
    return yksilot
        .findById(user.getId())
        .orElseThrow(() -> new NotFoundException("Profiili does not exist"));
  }

  public void update(
      JodUser user, YksiloDto dto, Authentication authentication, HttpServletRequest request) {
    var yksilo =
        yksilot
            .findById(user.getId())
            .orElseThrow(() -> new NotFoundException("Profiili does not exist"));
    var tervetuloapolku = dto.tervetuloapolku() != null ? dto.tervetuloapolku() : false;
    yksilo.setTervetuloapolku(tervetuloapolku);
    yksilot.save(yksilo);

    // Update the authentication object in the session
    var oldAuthentication = (Saml2Authentication) authentication;
    var principal = (JodSaml2Principal) oldAuthentication.getPrincipal();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new Saml2Authentication(
                new JodSaml2Principal(
                    principal.getName(),
                    principal.getAttributes(),
                    principal.getSessionIndexes(),
                    principal.getRelyingPartyRegistrationId(),
                    principal.getId(),
                    tervetuloapolku),
                oldAuthentication.getSaml2Response(),
                oldAuthentication.getAuthorities()));
    request
        .getSession()
        .setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
  }

  public void delete(JodUser user) {
    yksilot.deleteById(user.getId());
    yksilot.removeId(user.getId());
  }
}
