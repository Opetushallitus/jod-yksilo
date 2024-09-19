/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Service;

@Service
@DependsOnDatabaseInitialization
public class OsaaminenService {
  private static final Object SINGLETON_KEY = new Object();
  private final LoadingCache<Object, Map<URI, OsaaminenDto>> cache;

  public OsaaminenService(OsaaminenRepository osaamiset) {
    this.cache =
        Caffeine.newBuilder()
            .initialCapacity(1)
            .maximumSize(1)
            .refreshAfterWrite(Duration.ofMinutes(10))
            .build(k -> osaamiset.loadAll());
  }

  @PostConstruct
  public void init() {
    cache.refresh(SINGLETON_KEY);
  }

  public SivuDto<OsaaminenDto> findAll(int sivu, int koko) {
    var values = cache.get(SINGLETON_KEY).values();
    return new SivuDto<>(
        values.stream().skip((long) sivu * koko).limit(koko).toList(),
        values.size(),
        (values.size() + koko - 1) / koko);
  }

  public SivuDto<OsaaminenDto> findBy(int sivu, int koko, Set<URI> uri) {
    var values = findBy(uri);
    return new SivuDto<>(
        values.stream().skip((long) sivu * koko).limit(koko).toList(),
        values.size(),
        (values.size() + koko - 1) / koko);
  }

  public List<OsaaminenDto> findBy(Set<URI> uri) {
    var dtos = cache.get(SINGLETON_KEY);
    return uri.stream().map(dtos::get).toList();
  }

  public Map<URI, OsaaminenDto> getAll() {
    return cache.get(SINGLETON_KEY);
  }
}
