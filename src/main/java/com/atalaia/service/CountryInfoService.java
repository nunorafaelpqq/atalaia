package com.atalaia.service;

import com.atalaia.model.Models.CountryInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dados básicos do país: bandeira, capital, população, moeda...
 *
 * Os dados vêm de um ficheiro local (src/main/resources/data/countries.json), por isso não
 * dependem de nenhuma API externa. O ficheiro foi gerado a partir de mledoze/countries
 * (nomes, capitais, línguas, moedas...) + Banco Mundial (população, indicador SP.POP.TOTL).
 * A bandeira é carregada pelo browser a partir de flagcdn.com.
 */
@Service
public class CountryInfoService {

    private final Map<String, Entry> countries;

    public CountryInfoService(ObjectMapper json) throws IOException {
        try (InputStream in = new ClassPathResource("data/countries.json").getInputStream()) {
            this.countries = json.readValue(in, new TypeReference<Map<String, Entry>>() {
            });
        }
    }

    public CountryInfo get(String code) {
        Entry c = countries.get(code.toUpperCase(Locale.ROOT));
        if (c == null) {
            // Lança erro (→ WARN no log e fallback no frontend) em vez de devolver um painel vazio.
            throw new IllegalArgumentException("País desconhecido: " + code);
        }
        return new CountryInfo(
                code,
                c.pt(),
                c.name(),
                c.official(),
                "https://flagcdn.com/" + code.toLowerCase(Locale.ROOT) + ".svg",
                "Bandeira de " + c.pt(),
                c.capital() != null && !c.capital().isEmpty() ? String.join(", ", c.capital()) : null,
                c.region(),
                c.subregion(),
                c.population(),
                c.area(),
                c.languages() != null ? c.languages() : List.of(),
                c.currencies() != null ? c.currencies() : List.of(),
                List.of(),
                c.demonym(),
                "https://www.google.com/maps/search/?api=1&query="
                        + URLEncoder.encode(c.name(), StandardCharsets.UTF_8));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(String name, String official, String pt, List<String> capital, String region,
                 String subregion, long population, double area, List<String> languages,
                 List<String> currencies, String demonym) {
    }
}
