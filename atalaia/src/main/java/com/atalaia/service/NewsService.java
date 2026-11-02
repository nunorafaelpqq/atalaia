package com.atalaia.service;

import com.atalaia.model.Models.NewsItem;
import com.atalaia.model.Models.NewsResult;
import com.atalaia.support.RssParser;
import com.atalaia.support.TtlCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Notícias do país via RSS do Google News (grátis, sem chave).
 *
 * O Google News tem "edições" por país (~70). Se o país tiver edição, usamos a
 * edição local (notícias do próprio país). Se não tiver, pesquisamos o nome do
 * país nas notícias globais em inglês.
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private static final int LIMIT = 15;

    /** País -> {hl, ceid}. Acrescenta mais aqui à medida que testares. */
    private static final Map<String, String[]> EDITIONS = Map.ofEntries(
            Map.entry("PT", new String[]{"pt-PT", "PT:pt-150"}),
            Map.entry("BR", new String[]{"pt-BR", "BR:pt-419"}),
            Map.entry("US", new String[]{"en-US", "US:en"}),
            Map.entry("GB", new String[]{"en-GB", "GB:en"}),
            Map.entry("IE", new String[]{"en-IE", "IE:en"}),
            Map.entry("CA", new String[]{"en-CA", "CA:en"}),
            Map.entry("AU", new String[]{"en-AU", "AU:en"}),
            Map.entry("NZ", new String[]{"en-NZ", "NZ:en"}),
            Map.entry("IN", new String[]{"en-IN", "IN:en"}),
            Map.entry("ZA", new String[]{"en-ZA", "ZA:en"}),
            Map.entry("NG", new String[]{"en-NG", "NG:en"}),
            Map.entry("KE", new String[]{"en-KE", "KE:en"}),
            Map.entry("PH", new String[]{"en-PH", "PH:en"}),
            Map.entry("SG", new String[]{"en-SG", "SG:en"}),
            Map.entry("FR", new String[]{"fr", "FR:fr"}),
            Map.entry("DE", new String[]{"de", "DE:de"}),
            Map.entry("AT", new String[]{"de-AT", "AT:de"}),
            Map.entry("CH", new String[]{"de-CH", "CH:de"}),
            Map.entry("ES", new String[]{"es", "ES:es"}),
            Map.entry("MX", new String[]{"es-419", "MX:es-419"}),
            Map.entry("AR", new String[]{"es-419", "AR:es-419"}),
            Map.entry("CO", new String[]{"es-419", "CO:es-419"}),
            Map.entry("CL", new String[]{"es-419", "CL:es-419"}),
            Map.entry("PE", new String[]{"es-419", "PE:es-419"}),
            Map.entry("IT", new String[]{"it", "IT:it"}),
            Map.entry("NL", new String[]{"nl", "NL:nl"}),
            Map.entry("BE", new String[]{"nl", "BE:nl"}),
            Map.entry("PL", new String[]{"pl", "PL:pl"}),
            Map.entry("SE", new String[]{"sv", "SE:sv"}),
            Map.entry("NO", new String[]{"no", "NO:no"}),
            Map.entry("CZ", new String[]{"cs", "CZ:cs"}),
            Map.entry("HU", new String[]{"hu", "HU:hu"}),
            Map.entry("GR", new String[]{"el", "GR:el"}),
            Map.entry("RO", new String[]{"ro", "RO:ro"}),
            Map.entry("UA", new String[]{"uk", "UA:uk"}),
            Map.entry("RU", new String[]{"ru", "RU:ru"}),
            Map.entry("TR", new String[]{"tr", "TR:tr"}),
            Map.entry("IL", new String[]{"he", "IL:he"}),
            Map.entry("EG", new String[]{"ar", "EG:ar"}),
            Map.entry("SA", new String[]{"ar", "SA:ar"}),
            Map.entry("AE", new String[]{"ar", "AE:ar"}),
            Map.entry("JP", new String[]{"ja", "JP:ja"}),
            Map.entry("KR", new String[]{"ko", "KR:ko"}),
            Map.entry("CN", new String[]{"zh-CN", "CN:zh-Hans"}),
            Map.entry("TW", new String[]{"zh-TW", "TW:zh-Hant"}),
            Map.entry("TH", new String[]{"th", "TH:th"}),
            Map.entry("VN", new String[]{"vi", "VN:vi"}),
            Map.entry("ID", new String[]{"id", "ID:id"})
    );

    // Notícias mudam depressa: guardamos 20 minutos.
    private final TtlCache<NewsResult> cache = new TtlCache<>(Duration.ofMinutes(20));
    private final RestClient http;

    public NewsService(RestClient http) {
        this.http = http;
    }

    public NewsResult get(String code, String englishName) {
        return cache.get(code, () -> fetch(code, englishName));
    }

    private NewsResult fetch(String code, String englishName) {
        String[] edition = EDITIONS.get(code);
        if (edition != null) {
            try {
                String url = "https://news.google.com/rss?hl=" + edition[0] + "&gl=" + code + "&ceid=" + edition[1];
                List<NewsItem> items = RssParser.parseNews(download(url), LIMIT);
                if (!items.isEmpty()) {
                    return new NewsResult(NewsResult.LOCAL, items);
                }
            } catch (RuntimeException e) {
                log.warn("Edição local do Google News falhou para {}: {}", code, e.getMessage());
            }
        }
        // Fallback: pesquisa global pelo nome do país.
        String q = URLEncoder.encode("\"" + englishName + "\"", StandardCharsets.UTF_8);
        String url = "https://news.google.com/rss/search?q=" + q + "+when:2d&hl=en-US&gl=US&ceid=US:en";
        return new NewsResult(NewsResult.SEARCH, RssParser.parseNews(download(url), LIMIT));
    }

    private String download(String url) {
        return http.get().uri(URI.create(url)).retrieve().body(String.class);
    }
}
