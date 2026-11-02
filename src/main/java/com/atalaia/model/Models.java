package com.atalaia.model;

import java.util.List;
import java.util.Map;

/**
 * Os "objetos" que a API devolve ao frontend.
 * São records (Java 16+): classes imutáveis só com dados.
 * O Spring converte-os automaticamente para JSON.
 */
public final class Models {

    private Models() {
    }

    /** Dados básicos do país (vêm da REST Countries). */
    public record CountryInfo(
            String code,
            String name,          // nome em português
            String englishName,   // usado para pesquisar notícias / Wikipedia
            String officialName,
            String flagUrl,
            String flagAlt,
            String capital,
            String region,
            String subregion,
            long population,
            double area,
            List<String> languages,
            List<String> currencies,
            List<String> timezones,
            String demonym,       // ex.: "Portuguese"
            String mapsUrl) {
    }

    /** Uma notícia. */
    public record NewsItem(String title, String url, String source, String publishedAt) {
    }

    /** Resultado das notícias + de onde vieram. */
    public record NewsResult(String mode, List<NewsItem> items) {
        /** Edição local do Google News (notícias do próprio país, na língua do país). */
        public static final String LOCAL = "local";
        /** Fallback: pesquisa global pelo nome do país, em inglês. */
        public static final String SEARCH = "search";
    }

    /** Um tema em alta no Google Trends. */
    public record TrendItem(String title, String traffic, String url, String newsTitle, String newsUrl) {
    }

    /** Um acontecimento histórico. */
    public record HistoryEvent(int year, String text, String url) {
    }

    /** "Neste dia": eventos ligados ao país + alguns eventos mundiais. */
    public record OnThisDay(String date, List<HistoryEvent> countryEvents, List<HistoryEvent> worldEvents) {
    }

    /** Um jornal / site de notícias (lista curada à mão em data/newspapers.json). */
    public record Newspaper(String name, String url, String note) {
    }

    /** Tudo junto: é isto que /api/country/{code} devolve. */
    public record CountryPanel(
            String code,
            CountryInfo info,
            NewsResult news,
            List<TrendItem> trends,
            OnThisDay onThisDay,
            List<Newspaper> newspapers,
            Map<String, String> errors,
            String generatedAt) {
    }
}
