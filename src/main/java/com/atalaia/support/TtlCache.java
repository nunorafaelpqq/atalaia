package com.atalaia.support;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache simples em memória com prazo de validade (TTL).
 *
 * Porquê? As fontes (Google News, Wikipedia...) não gostam de muitos pedidos.
 * Se 100 pessoas clicarem em Portugal no mesmo minuto, só fazemos 1 pedido.
 *
 * Bónus: se a fonte falhar e tivermos uma versão antiga, devolvemos a antiga
 * em vez de mostrar erro.
 */
public class TtlCache<V> {

    private record Entry<T>(T value, Instant expiresAt) {
    }

    private final Map<String, Entry<V>> entries = new ConcurrentHashMap<>();
    private final Duration ttl;

    public TtlCache(Duration ttl) {
        this.ttl = ttl;
    }

    public V get(String key, Supplier<V> loader) {
        Entry<V> cached = entries.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.value();
        }
        try {
            V fresh = loader.get();
            entries.put(key, new Entry<>(fresh, Instant.now().plus(ttl)));
            return fresh;
        } catch (RuntimeException e) {
            if (cached != null) {
                return cached.value(); // versão antiga é melhor que nada
            }
            throw e;
        }
    }
}
