UPDATE yksilo
  SET lupa_kayttaa_tekoalyn_koulutukseen = lupa_luovuttaa_tiedot_ulkopuoliselle, 
    lupa_luovuttaa_tiedot_ulkopuoliselle = lupa_kayttaa_tekoalyn_koulutukseen
  WHERE (lupa_kayttaa_tekoalyn_koulutukseen IN (FALSE, TRUE) OR lupa_luovuttaa_tiedot_ulkopuoliselle IN (FALSE, TRUE));
