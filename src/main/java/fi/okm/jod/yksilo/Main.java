/*
 * Copyright (c) 2024 Finnish Ministry of Education and Culture,
 * Finnish Ministry of Economic Affairs and Employment.
 * Licensed under the EUPL-1.2-or-later.
 *
 * This file is part of jod-yksilo.
 */

package fi.okm.jod.yksilo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Application entrypoint. */
@SpringBootApplication
public class Main {

  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }
}
