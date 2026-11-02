package com.atalaia.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.atalaia.model.Models.CountryInfo;
import com.atalaia.support.TtlCache;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dados básicos do país: bandeira, capital, população, moeda...
 * Fonte: https://restcountries.com (grátis, sem chave).
 */
@Service
public class CountryInfoService {

    // Estes dados quase nunca mudam: guardamos 24 horas.´
    // test
    private final TtlCache<CountryInfo> cache = new TtlCache<>(Duration.ofHours(24));
    private final RestClient http;
    private final ObjectMapper json;

    public CountryInfoService(RestClient http, ObjectMapper json) {
        this.http = http;
        this.json = json;
    }

    public CountryInfo get(String code) {
        return cache.get(code, () -> fetch(code));
    }

    private CountryInfo fetch(String code) {
        String body = http.get()
                .uri(URI.create("https://restcountries.com/v3.1/alpha/" + code))
                .retrieve()
                .body(String.class);
        try {
            // A API às vezes devolve uma lista [ {...} ], às vezes só {...}.
            RcCountry c = body.trim().startsWith("[")
                    ? json.readValue(body, RcCountry[].class)[0]
                    : json.readValue(body, RcCountry.class);
            return toInfo(code, c);
        } catch (Exception e) {
            throw new IllegalStateException("Resposta inesperada da REST Countries", e);
        }
    }

    private static CountryInfo toInfo(String code, RcCountry c) {
        String english = c.name() != null ? c.name().common() : code;
        String portuguese = english;
        if (c.translations() != null && c.translations().get("por") != null) {
            portuguese = c.translations().get("por").common();
        }

        List<String> currencies = new ArrayList<>();
        if (c.currencies() != null) {
            c.currencies().forEach((iso, cur) ->
                    currencies.add(cur.name() + (cur.symbol() != null ? " (" + cur.symbol() + ")" : "")));
        }

        String demonym = null;
        if (c.demonyms() != null && c.demonyms().get("eng") != null) {
            demonym = c.demonyms().get("eng").m();
        }

        return new CountryInfo(
                code,
                portuguese,
                english,
                c.name() != null ? c.name().official() : null,
                c.flags() != null ? c.flags().svg() : null,
                c.flags() != null ? c.flags().alt() : null,
                c.capital() != null && !c.capital().isEmpty() ? String.join(", ", c.capital()) : null,
                c.region(),
                c.subregion(),
                c.population(),
                c.area(),
                c.languages() != null ? List.copyOf(c.languages().values()) : List.of(),
                currencies,
                c.timezones() != null ? c.timezones() : List.of(),
                demonym,
                c.maps() != null ? c.maps().get("googleMaps") : null);
    }

    // ---- Formato da resposta da REST Countries (só os campos que usamos) ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RcCountry(RcName name, List<String> capital, String region, String subregion,
                     long population, double area, Map<String, String> languages,
                     Map<String, RcCurrency> currencies, List<String> timezones, RcFlags flags,
                     Map<String, RcDemonym> demonyms, Map<String, String> maps,
                     Map<String, RcName> translations) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RcName(String common, String official) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RcCurrency(String name, String symbol) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RcFlags(String png, String svg, String alt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RcDemonym(String f, String m) {
    }
}
