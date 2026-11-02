package com.atalaia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação.
 * Corre isto e abre http://localhost:8080 no browser.
 */
@SpringBootApplication
public class AtalaiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtalaiaApplication.class, args);
    }
}
