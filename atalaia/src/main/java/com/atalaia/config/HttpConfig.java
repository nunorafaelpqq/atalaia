package com.atalaia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configura o cliente HTTP que usamos para ir buscar dados às fontes externas.
 */
@Configuration
public class HttpConfig {

    @Bean
    public RestClient sourcesClient(RestClient.Builder builder,
                                    @Value("${atalaia.user-agent}") String userAgent) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(8));

        return builder
                .requestFactory(factory)
                // A Wikipedia exige um User-Agent que identifique a app (ver application.properties).
                .defaultHeader("User-Agent", userAgent)
                .build();
    }
}
