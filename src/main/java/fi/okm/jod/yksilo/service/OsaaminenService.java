/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import fi.okm.jod.yksilo.domain.Versioned;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Service;

@Service
@DependsOnDatabaseInitialization
@Slf4j
public class OsaaminenService {

  private static final Object SINGLETON_KEY = new Object();
  private static final Versioned<Map<URI, OsaaminenDto>> EMPTY = new Versioned<>(0, Map.of());
  private final LoadingCache<Object, Versioned<Map<URI, OsaaminenDto>>> cache;

  static final Duration CACHE_DURATION = Duration.ofMinutes(1);

  @Autowired
  OsaaminenService(OsaaminenRepository osaamiset) {
    this(osaamiset, Ticker.systemTicker(), ForkJoinPool.commonPool());
  }

  // for testing
  OsaaminenService(OsaaminenRepository osaamiset, Ticker ticker, Executor executor) {
    this.cache =
        Caffeine.newBuilder()
            .ticker(ticker)
            .initialCapacity(1)
            .maximumSize(1)
            .executor(executor)
            .refreshAfterWrite(CACHE_DURATION)
            .build(new OsaaminenService.Loader(osaamiset));
  }

  public long currentVersion() {
    return getOsaamiset().version();
  }

  public Versioned<SivuDto<OsaaminenDto>> findAll(int sivu, int koko) {
    validateArgs(sivu, koko);
    var osaamiset = getOsaamiset();
    return new Versioned<>(osaamiset.version(), toPage(sivu, koko, osaamiset.payload().values()));
  }

  public Versioned<SivuDto<OsaaminenDto>> findBy(int sivu, int koko, Set<URI> uri) {
    validateArgs(sivu, koko);
    var osaamiset = getOsaamiset();
    var values =
        uri.stream().map(u -> osaamiset.payload().get(u)).filter(Objects::nonNull).toList();
    return new Versioned<>(osaamiset.version(), toPage(sivu, koko, values));
  }

  public List<OsaaminenDto> findBy(Set<URI> uri) {
    var osaamiset = getOsaamiset().payload();
    return uri.stream().map(osaamiset::get).filter(Objects::nonNull).toList();
  }

  public Map<URI, OsaaminenDto> getAll() {
    return getOsaamiset().payload();
  }

  private Versioned<Map<URI, OsaaminenDto>> getOsaamiset() {
    var value = cache.get(SINGLETON_KEY);
    return value == null ? EMPTY : value; // NOSONAR
  }

  private static void validateArgs(int sivu, int koko) {
    if (sivu < 0 || koko < 1) {
      throw new IllegalArgumentException("Invalid page or size");
    }
  }

  private static SivuDto<OsaaminenDto> toPage(int sivu, int koko, Collection<OsaaminenDto> values) {
    return new SivuDto<>(
        values.stream().skip((long) sivu * koko).limit(koko).toList(),
        values.size(),
        (values.size() + koko - 1) / koko);
  }

  @RequiredArgsConstructor
  private static class Loader
      implements CacheLoader<@NonNull Object, Versioned<Map<URI, OsaaminenDto>>> {
    private final OsaaminenRepository osaamiset;

    @Override
    public Versioned<Map<URI, OsaaminenDto>> load(Object key) {
      return osaamiset.refreshAll(null);
    }

    @Override
    public Versioned<Map<URI, OsaaminenDto>> reload(
        Object key, @NonNull Versioned<Map<URI, OsaaminenDto>> oldValue) {
      return osaamiset.refreshAll(oldValue);
    }
  }
}
