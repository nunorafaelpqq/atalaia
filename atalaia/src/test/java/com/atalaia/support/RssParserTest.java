package com.atalaia.support;

import com.atalaia.model.Models.NewsItem;
import com.atalaia.model.Models.TrendItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes ao leitor de RSS, com exemplos no formato do Google.
 * Corre com: mvn test   (ou o botão ▶ ao lado do nome da classe no VS Code)
 */
class RssParserTest {

    static final String NEWS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel><title>Google News</title>
              <item>
                <title>Governo aprova Orçamento - Público</title>
                <link>https://news.google.com/rss/articles/abc</link>
                <pubDate>Mon, 02 Nov 2026 19:00:00 GMT</pubDate>
                <source url="https://www.publico.pt">Público</source>
              </item>
              <item>
                <title>Benfica vence em casa - Record</title>
                <link>https://news.google.com/rss/articles/def</link>
                <pubDate>Mon, 02 Nov 2026 18:30:00 GMT</pubDate>
                <source url="https://www.record.pt">Record</source>
              </item>
            </channel></rss>
            """;

    static final String TRENDS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss xmlns:ht="https://trends.google.com/trending/rss" version="2.0"><channel>
              <item>
                <title>benfica</title>
                <ht:approx_traffic>20000+</ht:approx_traffic>
                <ht:news_item>
                  <ht:news_item_title>Benfica vence em casa</ht:news_item_title>
                  <ht:news_item_url>https://www.record.pt/x</ht:news_item_url>
                </ht:news_item>
              </item>
            </channel></rss>
            """;

    @Test
    void parsesNewsAndRemovesSourceFromTitle() {
        List<NewsItem> items = RssParser.parseNews(NEWS, 10);
        assertEquals(2, items.size());
        assertEquals("Governo aprova Orçamento", items.get(0).title());
        assertEquals("Público", items.get(0).source());
        assertEquals("2026-11-02T19:00:00Z", items.get(0).publishedAt());
    }

    @Test
    void respectsLimit() {
        assertEquals(1, RssParser.parseNews(NEWS, 1).size());
    }

    @Test
    void parsesTrends() {
        List<TrendItem> trends = RssParser.parseTrends(TRENDS, "PT", 10);
        assertEquals(1, trends.size());
        assertEquals("benfica", trends.get(0).title());
        assertEquals("20000+", trends.get(0).traffic());
        assertEquals("Benfica vence em casa", trends.get(0).newsTitle());
        assertTrue(trends.get(0).url().contains("geo=PT"));
    }

    @Test
    void rejectsXmlWithDoctype() {
        String evil = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><rss><item><title>&e;</title></item></rss>";
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> RssParser.parseNews(evil, 5));
    }
}
