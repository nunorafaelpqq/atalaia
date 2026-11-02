package com.atalaia.support;

import com.atalaia.model.Models.NewsItem;
import com.atalaia.model.Models.TrendItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê feeds RSS (XML) do Google News e do Google Trends.
 * Usa só o que vem com o Java, sem bibliotecas extra.
 */
public final class RssParser {

    private RssParser() {
    }

    /** Google News: cada <item> tem title, link, pubDate e source. */
    public static List<NewsItem> parseNews(String xml, int limit) {
        List<NewsItem> result = new ArrayList<>();
        for (Element item : items(xml)) {
            if (result.size() >= limit) {
                break;
            }
            String title = childText(item, "title");
            String source = childText(item, "source");
            // O Google acrescenta " - Nome do Jornal" ao título; tiramos para não repetir.
            if (source != null && title != null && title.endsWith(" - " + source)) {
                title = title.substring(0, title.length() - source.length() - 3);
            }
            if (title == null || title.isBlank()) {
                continue;
            }
            result.add(new NewsItem(title, childText(item, "link"), source, toIso(childText(item, "pubDate"))));
        }
        return result;
    }

    /** Google Trends: cada <item> é um tema; tem tráfego aproximado e notícias relacionadas. */
    public static List<TrendItem> parseTrends(String xml, String geo, int limit) {
        List<TrendItem> result = new ArrayList<>();
        for (Element item : items(xml)) {
            if (result.size() >= limit) {
                break;
            }
            String title = childText(item, "title");
            if (title == null || title.isBlank()) {
                continue;
            }
            String newsTitle = null;
            String newsUrl = null;
            NodeList newsItems = item.getElementsByTagNameNS("*", "news_item");
            if (newsItems.getLength() > 0) {
                Element first = (Element) newsItems.item(0);
                newsTitle = childText(first, "news_item_title");
                newsUrl = childText(first, "news_item_url");
            }
            String exploreUrl = "https://trends.google.com/trends/explore?geo=" + geo + "&q="
                    + URLEncoder.encode(title, StandardCharsets.UTF_8);
            result.add(new TrendItem(title, childText(item, "approx_traffic"), exploreUrl, newsTitle, newsUrl));
        }
        return result;
    }

    // ---------- auxiliares ----------

    private static List<Element> items(String xml) {
        Document doc = parse(xml);
        NodeList nodes = doc.getElementsByTagName("item");
        List<Element> list = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            list.add((Element) nodes.item(i));
        }
        return list;
    }

    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Segurança: bloqueia ataques XXE (XML com entidades externas maliciosas).
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalStateException("RSS inválido", e);
        }
    }

    /** Texto do primeiro filho direto com este nome (ignora prefixos tipo "ht:"). */
    private static String childText(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String name = n.getLocalName() != null ? n.getLocalName() : n.getNodeName();
            if (name.equals(localName)) {
                String text = n.getTextContent();
                return text == null ? null : text.trim();
            }
        }
        return null;
    }

    /** "Mon, 02 Nov 2026 19:00:00 GMT" -> "2026-11-02T19:00Z" */
    private static String toIso(String rfc1123) {
        if (rfc1123 == null) {
            return null;
        }
        try {
            return ZonedDateTime.parse(rfc1123, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toString();
        } catch (Exception e) {
            return null;
        }
    }
}
