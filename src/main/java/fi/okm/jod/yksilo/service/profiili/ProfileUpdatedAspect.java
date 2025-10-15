/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.entity.Yksilo;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;

@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ProfileUpdatedAspect {
  private final EntityManager entityManager;

  @Pointcut("within(fi.okm.jod.yksilo.service.profiili.*) && args(user,..)")
  public void profiili(JodUser user) {}

  @Pointcut(
      """
      @within(org.springframework.transaction.annotation.Transactional) ||
      @annotation(org.springframework.transaction.annotation.Transactional)
      """)
  public void transactional() {}

  @Around(value = "profiili(user) && transactional()", argNames = "joinPoint,user")
  public Object updateAspect(ProceedingJoinPoint joinPoint, JodUser user) throws Throwable {
    if (isSynchronizationActive() && !isCurrentTransactionReadOnly()) {
      final var signature = joinPoint.getSignature();
      final var operation =
          signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
      registerSynchronization(
          new SynchronizationCallback(
              entityManager.getReference(Yksilo.class, user.getId()), operation));
    }
    return joinPoint.proceed();
  }

  @RequiredArgsConstructor
  static class SynchronizationCallback implements TransactionSynchronization {
    private final Yksilo yksilo;
    private final String operation;

    @Override
    public void beforeCommit(boolean readOnly) {
      if (!readOnly) {
        yksilo.updated();
        log.atInfo()
            .addKeyValue("operation", operation)
            .log("User {} profile updated", yksilo.getId());
      }
    }
  }
}
