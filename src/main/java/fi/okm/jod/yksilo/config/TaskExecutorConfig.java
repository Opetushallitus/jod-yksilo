/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import java.util.List;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;

@Configuration
public class TaskExecutorConfig {
  @Bean
  @DependsOn("entityManagerFactory") // Ensure that entity manager outlives the executor
  TaskExecutor taskExecutor(SimpleAsyncTaskExecutorBuilder builder) {
    var decorator =
        new CompositeTaskDecorator(
            List.of(
                (TaskDecorator) DelegatingSecurityContextRunnable::new,
                new ContextPropagatingTaskDecorator()));
    return builder.taskDecorator(decorator).build();
  }
}
