/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static fi.okm.jod.yksilo.service.profiili.Mapper.cachingMapper;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.KategoriaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKategoriaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusUpdateResultDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKategoria;
import fi.okm.jod.yksilo.entity.Koulutus_;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.KoulutusKategoriaRepository;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class KoulutusService {
  private final KoulutusRepository koulutukset;
  private final YksiloRepository yksilot;
  private final KoulutusKategoriaRepository kategoriat;
  private final YksilonOsaaminenService yksilonOsaamiset;

  private static List<KoulutusKategoriaDto> groupByKategoria(List<Koulutus> result) {
    var kategoriaMapper = cachingMapper(Mapper::mapKategoria);

    return result.stream()
        .collect(
            groupingBy(
                k -> Optional.ofNullable(kategoriaMapper.apply(k.getKategoria())),
                mapping(Mapper::mapKoulutus, toSet())))
        .entrySet()
        .stream()
        .flatMap(
            e ->
                e.getKey()
                    .map(key -> Stream.of(new KoulutusKategoriaDto(key, e.getValue())))
                    .orElseGet(
                        () ->
                            e.getValue().stream()
                                .map(k -> new KoulutusKategoriaDto(null, Set.of(k)))))
        .toList();
  }

  private static void validateKategoriaNotEmpty(KategoriaDto kategoriaDto, Set<KoulutusDto> dtos) {
    if ((dtos == null || dtos.isEmpty()) && kategoriaDto != null && kategoriaDto.id() == null) {
      throw new ServiceValidationException("Empty Kategoria not created");
    }
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }

  public List<KoulutusKategoriaDto> findAll(JodUser user) {
    final Yksilo yksilo = yksilot.getReferenceById(user.getId());
    var sort = Sort.by(Koulutus_.KATEGORIA, Koulutus_.ALKU_PVM, Koulutus_.ID);
    return groupByKategoria(koulutukset.findByYksilo(yksilo, sort));
  }

  public KoulutusDto getKoulutus(JodUser user, UUID koulutusId) {
    return koulutukset
        .findByYksiloIdAndId(user.getId(), koulutusId)
        .map(Mapper::mapKoulutus)
        .orElseThrow(KoulutusService::notFound);
  }

  public KategoriaDto getKategoria(JodUser user, UUID kategoriaId) {
    return kategoriat
        .findByYksiloAndId(yksilot.getReferenceById(user.getId()), kategoriaId)
        .map(Mapper::mapKategoria)
        .orElseThrow(KoulutusService::notFound);
  }

  public List<KoulutusKategoriaDto> findAll(JodUser user, UUID kategoriaId) {
    final Yksilo yksilo = yksilot.getReferenceById(user.getId());
    var sort = Sort.by(Koulutus_.KATEGORIA, Koulutus_.ALKU_PVM, Koulutus_.ID);
    return groupByKategoria(koulutukset.findByYksiloAndKategoriaId(yksilo, kategoriaId, sort));
  }

  public KoulutusUpdateResultDto merge(
      JodUser user, KategoriaDto kategoriaDto, Set<KoulutusDto> dtos) {
    return merge(user, kategoriaDto, dtos, false);
  }

  public KoulutusUpdateResultDto upsert(
      JodUser user, KategoriaDto kategoriaDto, Set<KoulutusDto> dtos) {
    return merge(user, kategoriaDto, dtos, true);
  }

  @SuppressWarnings("java:S3776")
  private KoulutusUpdateResultDto merge(
      JodUser user, KategoriaDto kategoriaDto, Set<KoulutusDto> dtos, boolean partial) {

    validateKategoriaNotEmpty(kategoriaDto, dtos);

    final var yksilo = yksilot.getReferenceById(user.getId());

    final var kategoria = resolve(yksilo, kategoriaDto);
    if (kategoria != null && kategoriaDto.nimi() != null) {
      kategoria.setNimi(kategoriaDto.nimi());
      kategoria.setKuvaus(kategoriaDto.kuvaus());
    }

    final Set<UUID> touched = dtos == null ? Set.of() : HashSet.newHashSet(dtos.size());

    if (dtos != null) {

      boolean move = false;

      for (var dto : dtos) {
        var koulutus = resolve(yksilo, dto);

        if (koulutus.getId() != null && !Objects.equals(koulutus.getKategoria(), kategoria)) {
          move = true;
        }

        koulutus.setKategoria(kategoria);
        koulutus.setNimi(dto.nimi());
        koulutus.setKuvaus(dto.kuvaus());
        koulutus.setAlkuPvm(dto.alkuPvm());
        koulutus.setLoppuPvm(dto.loppuPvm());
        touched.add(koulutukset.save(koulutus).getId());

        if (dto.osaamiset() != null) {
          yksilonOsaamiset.update(koulutus, dto.osaamiset());
        }
      }

      if (!move && !partial && kategoria != null) {
        var removed = koulutukset.findByKategoriaAndIdNotIn(kategoria, touched);
        if (!removed.isEmpty()) {
          koulutukset.deleteOsaamiset(removed.stream().map(Koulutus::getId).toList());
          koulutukset.deleteAll(removed);
        }
      }

      koulutukset.flush();

      if (koulutukset.countByYksilo(yksilo) > Limits.KOULUTUS) {
        throw new ServiceValidationException("Limit for number of Koulutus exceeded");
      }

      kategoriat.deleteOrphaned(yksilo);
    }

    return new KoulutusUpdateResultDto(kategoria == null ? null : kategoria.getId(), touched);
  }

  public void deleteKoulutukset(JodUser user, Set<UUID> ids) {
    var yksilo = yksilot.getReferenceById(user.getId());
    var entities = koulutukset.findByYksiloAndIdIn(yksilo, ids);

    if (entities.size() != ids.size()) {
      throw new NotFoundException("Koulutus not found");
    }

    koulutukset.deleteOsaamiset(ids);
    koulutukset.deleteAll(entities);
    kategoriat.deleteOrphaned(yksilo);
  }

  private KoulutusKategoria resolve(Yksilo yksilo, KategoriaDto dto) {
    if (dto == null) {
      return null;
    }

    // Maybe optimize using a Query (probably unnecessary if there are only few Kategorias)
    var existing =
        kategoriat.findAllByYksilo(yksilo).stream()
            .filter(e -> e.getNimi().equals(dto.nimi()) || e.getId().equals(dto.id()))
            .toList();

    if (existing.size() > 1) {
      throw new ServiceValidationException("Duplicate Kategoria");
    }

    if (existing.isEmpty()) {
      if (dto.id() == null) {
        return kategoriat.save(new KoulutusKategoria(yksilo, dto.nimi(), dto.kuvaus()));
      } else {
        throw new ServiceValidationException("Kategoria not found");
      }
    }

    return existing.getFirst();
  }

  private Koulutus resolve(Yksilo yksilo, KoulutusDto dto) {
    if (dto.id() != null) {
      return koulutukset
          .findByYksiloIdAndId(yksilo.getId(), dto.id())
          .orElseThrow(() -> new ServiceValidationException("Koulutus not found"));
    } else {
      return new Koulutus(yksilo);
    }
  }

  public List<KategoriaDto> getKategoriat(JodUser user) {
    return kategoriat.findAllByYksilo(yksilot.getReferenceById(user.getId())).stream()
        .map(Mapper::mapKategoria)
        .toList();
  }

  public void update(JodUser user, KoulutusDto dto) {
    var koulutus =
        koulutukset
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(KoulutusService::notFound);

    koulutus.setAlkuPvm(dto.alkuPvm());
    koulutus.setLoppuPvm(dto.loppuPvm());
    koulutus.setNimi(dto.nimi());
    koulutus.setKuvaus(dto.kuvaus());
    if (dto.osaamiset() != null) {
      yksilonOsaamiset.update(koulutus, dto.osaamiset());
    }
  }

  public void updateKategoria(JodUser user, KategoriaDto dto) {
    requireNonNull(dto);
    requireNonNull(dto.id());

    var kategoria = resolve(yksilot.getReferenceById(user.getId()), dto);
    kategoria.setNimi(dto.nimi());
    kategoria.setKuvaus(dto.kuvaus());
  }
}
