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
import org.aspectj.lang.JoinPoint;
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

  @Pointcut("within(fi.okm.jod.yksilo.service.profiili..*)")
  public void profiili() {}

  @Pointcut(
      """
      @within(org.springframework.transaction.annotation.Transactional) ||
      @annotation(org.springframework.transaction.annotation.Transactional)
      """)
  public void transactional() {}

  @Around(value = "profiili() && transactional()", argNames = "joinPoint")
  public Object updateAspect(ProceedingJoinPoint joinPoint) throws Throwable {
    if (user(joinPoint) instanceof JodUser user
        && isSynchronizationActive()
        && !isCurrentTransactionReadOnly()) {
      final var signature = joinPoint.getSignature();
      final var operation =
          signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
      registerSynchronization(
          new SynchronizationCallback(
              entityManager.getReference(Yksilo.class, user.getId()), operation));
    }
    return joinPoint.proceed();
  }

  // args(.., user, ..) is not a valid pointcut expression
  private JodUser user(JoinPoint joinPoint) {
    JodUser user = null;
    for (var arg : joinPoint.getArgs()) {
      if (arg instanceof JodUser userArg) {
        if (user != null) {
          log.warn(
              "Multiple JodUser arguments in method {}, cannot determine user",
              joinPoint.getSignature());
          return null;
        } else {
          user = userArg;
        }
      }
    }
    return user;
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
