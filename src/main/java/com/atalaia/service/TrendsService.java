package com.atalaia.service;

import com.atalaia.model.Models.TrendItem;
import com.atalaia.support.RssParser;
import com.atalaia.support.TtlCache;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * O que as pessoas estão a pesquisar no Google neste país (Google Trends).
 * Nem todos os países têm Trends; nesses casos devolve lista vazia.
 */
@Service
public class TrendsService {

    private final TtlCache<List<TrendItem>> cache = new TtlCache<>(Duration.ofMinutes(30));
    private final RestClient http;

    public TrendsService(RestClient http) {
        this.http = http;
    }

    public List<TrendItem> get(String code) {
        return cache.get(code, () -> {
            String xml = http.get()
                    .uri(URI.create("https://trends.google.com/trending/rss?geo=" + code))
                    .retrieve()
                    .body(String.class);
            return RssParser.parseTrends(xml, code, 12);
        });
    }
}
