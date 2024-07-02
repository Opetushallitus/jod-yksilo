/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.entity.Kuva;
import fi.okm.jod.yksilo.repository.KuvaRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class KuvaService {
  private final YksiloRepository yksilot;
  private final KuvaRepository kuvat;

  public Kuva add(JodUser user, MultipartFile file) throws IOException {
    final var yksilo = yksilot.getReferenceById(user.getId());
    var kuva = new Kuva(yksilo);
    kuva.setTyyppi(file.getContentType());

    // Resize image to 320x320
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Thumbnails.of(file.getInputStream()).size(320, 320).toOutputStream(baos);

    kuva.setData(baos.toByteArray());
    kuva = kuvat.save(kuva);
    return kuva;
  }

  public Kuva find(JodUser user, UUID id) {
    return kuvat.findByYksiloIdAndId(user.getId(), id).orElseThrow();
  }
}
