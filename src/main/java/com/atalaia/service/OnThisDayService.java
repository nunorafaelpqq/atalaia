package com.atalaia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.atalaia.model.Models.HistoryEvent;
import com.atalaia.model.Models.OnThisDay;
import com.atalaia.support.TtlCache;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * "Neste dia": acontecimentos históricos de hoje (Wikipedia, em inglês),
 * filtrados pelos que mencionam o país.
 */
@Service
public class OnThisDayService {

    // Os eventos do dia são iguais para todos os países: 1 pedido serve todos.
    private final TtlCache<List<WikiEvent>> cache = new TtlCache<>(Duration.ofHours(6));
    private final RestClient http;
    private final ObjectMapper json;

    public OnThisDayService(RestClient http, ObjectMapper json) {
        this.http = http;
        this.json = json;
    }

    public OnThisDay get(String englishName, String demonym) {
        LocalDate today = LocalDate.now();
        String mm = String.format("%02d", today.getMonthValue());
        String dd = String.format("%02d", today.getDayOfMonth());
        List<WikiEvent> all = cache.get(mm + "-" + dd, () -> fetch(mm, dd));

        Pattern match = buildPattern(englishName, demonym);
        List<HistoryEvent> country = new ArrayList<>();
        List<HistoryEvent> world = new ArrayList<>();
        for (WikiEvent e : all) {
            HistoryEvent h = new HistoryEvent(e.year(), e.text(), e.url());
            if (match != null && mentions(e, match)) {
                country.add(h);
            } else if (world.size() < 3) {
                world.add(h);
            }
        }
        country.sort(Comparator.comparingInt(HistoryEvent::year).reversed());
        return new OnThisDay(today.toString(), country.stream().limit(8).toList(), world);
    }

    /**
     * Lemos a resposta como texto e navegamos no JSON "à mão" (JsonNode).
     * É mais tolerante do que mapear para classes: se a Wikipedia mudar ou
     * omitir um campo, simplesmente fica vazio em vez de rebentar.
     */
    private List<WikiEvent> fetch(String mm, String dd) {
        String body = http.get()
                .uri(URI.create("https://en.wikipedia.org/api/rest_v1/feed/onthisday/events/" + mm + "/" + dd))
                .retrieve()
                .body(String.class);

        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("Resposta inesperada da Wikipedia", e);
        }

        List<WikiEvent> events = new ArrayList<>();
        for (JsonNode ev : root.path("events")) {
            String text = ev.path("text").asText(null);
            if (text == null) {
                continue;
            }
            List<String> titles = new ArrayList<>();
            String url = null;
            for (JsonNode page : ev.path("pages")) {
                String title = page.path("titles").path("normalized").asText(null);
                if (title != null) {
                    titles.add(title);
                }
                if (url == null) {
                    url = page.path("content_urls").path("desktop").path("page").asText(null);
                }
            }
            events.add(new WikiEvent(ev.path("year").asInt(0), text, titles, url));
        }
        return events;
    }

    /** \b = fronteira de palavra, para "Niger" não apanhar "Nigeria". */
    private static Pattern buildPattern(String name, String demonym) {
        List<String> words = new ArrayList<>();
        if (name != null && !name.isBlank()) {
            words.add(Pattern.quote(name));
        }
        if (demonym != null && !demonym.isBlank()) {
            words.add(Pattern.quote(demonym));
        }
        if (words.isEmpty()) {
            return null;
        }
        return Pattern.compile("\\b(" + String.join("|", words) + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    private static boolean mentions(WikiEvent e, Pattern p) {
        if (p.matcher(e.text()).find()) {
            return true;
        }
        for (String title : e.pageTitles()) {
            if (p.matcher(title).find()) {
                return true;
            }
        }
        return false;
    }

    /** Um evento, já simplificado. */
    record WikiEvent(int year, String text, List<String> pageTitles, String url) {
    }
}
