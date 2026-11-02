package com.atalaia.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.atalaia.model.Models.Newspaper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Principais jornais/sites de cada país.
 * Lista feita à mão em src/main/resources/data/newspapers.json — acrescenta países lá!
 */
@Service
public class NewspaperService {

    private final Map<String, List<Newspaper>> byCountry;

    public NewspaperService(ObjectMapper json) throws IOException {
        try (InputStream in = new ClassPathResource("data/newspapers.json").getInputStream()) {
            this.byCountry = json.readValue(in, new TypeReference<Map<String, List<Newspaper>>>() {
            });
        }
    }

    public List<Newspaper> get(String code) {
        return byCountry.getOrDefault(code, List.of());
    }
}
