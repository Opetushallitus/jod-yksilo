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
import fi.okm.jod.yksilo.dto.AmmattiDto;
import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.repository.AmmattiRepository;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Service;

@Service
@DependsOnDatabaseInitialization
@Slf4j
public class AmmattiService {
  private static final Object SINGLETON_KEY = new Object();
  private final LoadingCache<Object, Versioned<Map<URI, AmmattiDto>>> cache;

  private final AmmattiRepository ammatit;

  static final Duration CACHE_DURATION = Duration.ofMinutes(1);

  @Autowired
  AmmattiService(AmmattiRepository ammatit) {
    this(ammatit, Ticker.systemTicker(), ForkJoinPool.commonPool());
  }

  // for testing
  AmmattiService(AmmattiRepository ammatit, Ticker ticker, Executor executor) {
    this.ammatit = ammatit;
    this.cache =
        Caffeine.newBuilder()
            .ticker(ticker)
            .initialCapacity(1)
            .maximumSize(1)
            .executor(executor)
            .refreshAfterWrite(CACHE_DURATION)
            .build(new Loader(this.ammatit));
  }

  public long currentVersion() {
    return cache.get(SINGLETON_KEY).version();
  }

  public Versioned<SivuDto<AmmattiDto>> findAll(int sivu, int koko) {
    var value = cache.get(SINGLETON_KEY);
    var values = value.payload().values();
    var result =
        new SivuDto<>(
            values.stream().skip((long) sivu * koko).limit(koko).toList(),
            values.size(),
            (values.size() + koko - 1) / koko);
    return new Versioned<>(value.version(), result);
  }

  public Versioned<SivuDto<AmmattiDto>> findBy(int sivu, int koko, Set<URI> uri) {
    var value = cache.get(SINGLETON_KEY);
    var values = uri.stream().map(u -> value.payload().get(u)).filter(Objects::nonNull).toList();
    var payload =
        new SivuDto<>(
            values.stream().skip((long) sivu * koko).limit(koko).toList(),
            values.size(),
            (values.size() + koko - 1) / koko);
    return new Versioned<>(value.version(), payload);
  }

  public List<AmmattiDto> findBy(Set<URI> uri) {
    var dtos = cache.get(SINGLETON_KEY).payload();
    return uri.stream().map(dtos::get).filter(Objects::nonNull).toList();
  }

  public Map<URI, AmmattiDto> getAll() {
    return cache.get(SINGLETON_KEY).payload();
  }

  @RequiredArgsConstructor
  private static class Loader implements CacheLoader<Object, Versioned<Map<URI, AmmattiDto>>> {
    private final AmmattiRepository ammatit;

    @Override
    public Versioned<Map<URI, AmmattiDto>> load(Object key) throws Exception {
      return reload(key, null);
    }

    @Override
    public @Nullable Versioned<Map<URI, AmmattiDto>> reload(
        Object key, Versioned<Map<URI, AmmattiDto>> oldValue) throws Exception {
      return ammatit.refreshAll(oldValue);
    }
  }
}
