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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    var mediaType = MediaType.parseMediaType(Objects.requireNonNull(file.getContentType()));
    kuva.setTyyppi(mediaType.toString());
    var baos = new ByteArrayOutputStream();
    BufferedImage image = ImageIO.read(file.getInputStream());
    ImageIO.write(image, mediaType.getSubtype(), baos);
    kuva.setData(baos.toByteArray());
    kuva = kuvat.save(kuva);
    return kuva;
  }

  public Kuva find(JodUser user, UUID id) {
    return kuvat.findByYksiloIdAndId(user.getId(), id).orElseThrow();
  }
}
