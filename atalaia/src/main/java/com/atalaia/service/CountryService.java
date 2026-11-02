package com.atalaia.service;

import com.atalaia.model.Models.CountryInfo;
import com.atalaia.model.Models.CountryPanel;
import com.atalaia.model.Models.NewsResult;
import com.atalaia.model.Models.OnThisDay;
import com.atalaia.model.Models.TrendItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Junta tudo para um país.
 *
 * Cada secção é pedida EM PARALELO (não esperamos que uma acabe para começar a outra),
 * e se uma fonte falhar, as outras continuam a aparecer.
 */
@Service
public class CountryService {

    private static final Logger log = LoggerFactory.getLogger(CountryService.class);

    // Virtual threads (Java 21): threads leves, ótimas para esperar por pedidos HTTP.
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final CountryInfoService infoService;
    private final NewsService newsService;
    private final TrendsService trendsService;
    private final OnThisDayService onThisDayService;
    private final NewspaperService newspaperService;

    public CountryService(CountryInfoService infoService, NewsService newsService, TrendsService trendsService,
                          OnThisDayService onThisDayService, NewspaperService newspaperService) {
        this.infoService = infoService;
        this.newsService = newsService;
        this.trendsService = trendsService;
        this.onThisDayService = onThisDayService;
        this.newspaperService = newspaperService;
    }

    /**
     * @param code     código ISO de 2 letras (PT, BR, US...)
     * @param nameHint nome em inglês enviado pelo mapa, usado se a REST Countries falhar
     */
    public CountryPanel panel(String code, String nameHint) {
        Map<String, String> errors = new ConcurrentHashMap<>();

        CompletableFuture<CountryInfo> infoF = async("info", errors, () -> infoService.get(code), null);
        CompletableFuture<List<TrendItem>> trendsF = async("trends", errors, () -> trendsService.get(code), List.of());

        // Notícias e "neste dia" precisam do nome do país, por isso esperam pela info.
        CompletableFuture<NewsResult> newsF = infoF.thenCompose(info ->
                async("news", errors, () -> newsService.get(code, englishName(info, nameHint)), null));
        CompletableFuture<OnThisDay> historyF = infoF.thenCompose(info ->
                async("onThisDay", errors,
                        () -> onThisDayService.get(englishName(info, nameHint), info != null ? info.demonym() : null),
                        null));

        CompletableFuture.allOf(trendsF, newsF, historyF).join();

        return new CountryPanel(
                code,
                infoF.join(),
                newsF.join(),
                trendsF.join(),
                historyF.join(),
                newspaperService.get(code),
                new TreeMap<>(errors),
                Instant.now().toString());
    }

    /** Corre uma tarefa noutra thread; se falhar, regista o erro e devolve o valor por omissão. */
    private <T> CompletableFuture<T> async(String section, Map<String, String> errors, Supplier<T> task, T fallback) {
        return CompletableFuture.supplyAsync(task, executor).exceptionally(ex -> {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause(); // vai até à causa original do erro
            }
            log.warn("Fonte '{}' falhou: {}", section, root.toString());
            errors.put(section, "Fonte indisponível de momento");
            return fallback;
        });
    }

    private static String englishName(CountryInfo info, String nameHint) {
        if (info != null && info.englishName() != null) {
            return info.englishName();
        }
        return nameHint;
    }
}
