package com.atalaia.web;

import com.atalaia.model.Models.CountryPanel;
import com.atalaia.service.CountryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * A API que o frontend chama.
 * Experimenta no browser: http://localhost:8080/api/country/PT
 */
@RestController
@RequestMapping("/api")
public class CountryController {

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping("/country/{code}")
    public ResponseEntity<CountryPanel> country(@PathVariable("code") String code,
                                                @RequestParam(name = "name", required = false) String name) {
        if (code == null || !code.matches("[A-Za-z]{2}")) {
            return ResponseEntity.badRequest().build();
        }
        if (name != null && name.length() > 80) {
            name = name.substring(0, 80);
        }
        return ResponseEntity.ok(countryService.panel(code.toUpperCase(Locale.ROOT), name));
    }
}
