/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.mocklogin;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ConditionalOnBean(MockLoginConfig.class)
@Hidden
class MockLoginController {

  @GetMapping(value = "/login", produces = "text/html")
  @ResponseBody
  public String login(CsrfToken csrf) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>JOD Mock Login Page</title>
          <style>
            body {
              font-size: 16px;
              font-family: Arial, sans-serif;
              background-color: #f4f4f4;
              display: flex;
              justify-content: center;
              align-items: center;
              margin: 20px;
            }

            .login-container {
              background-color: #fff;
              padding: 20px;
              border-radius: 8px;
              box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
              width: 300px;
            }

            .login-container h2 {
              font-size: 24px;
              margin: 10px auto 30px;
              text-align: center;
              box-sizing: border-box;
            }

            .login-container label {
              display: block;
              margin-bottom: 5px;
              font-weight: bold;
            }

            .login-container input[type="text"],
            .login-container input[type="password"] {
              padding: 10px;
              width: 100%;
              margin-bottom: 15px;
              border: 1px solid #ccc;
              border-radius: 4px;
              font-size: 16px;
              box-sizing: border-box;
            }

            .login-container button {
              width: 100%;
              padding: 10px;
              background-color: rgb(0, 109, 179);
              border: none;
              border-radius: 4px;
              color: #fff;
              font-size: 16px;
              cursor: pointer;
            }

            .login-container button:hover {
              background-color: rgb(0, 87, 143);
            }
          </style>
        </head>
        <body>
        <div class="login-container">
          <h2>JOD Yksilö Mock Login</h2>
          <form method="post" action="/login">
            <label for="username">Username</label>
            <input type="text" id="username" name="username" required>
            <label for="password">Password</label>
            <input type="password" id="password" name="password" required>
            <button type="submit">Sign In</button>
            <input type="hidden" name="_csrf" value="{csrf}">
          </form>
        </div>
        </body>
        </html>
        """
        .replace("{csrf}", csrf.getToken());
  }
}
