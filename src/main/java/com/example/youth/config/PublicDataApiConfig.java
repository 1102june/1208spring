package com.example.youth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PublicDataApiConfig {

    @Value("${public-data.lh.rental-house-list.url}")
    private String lhRentalHouseListUrl;

    @Bean(name = "lhWebClient")
    public WebClient lhWebClient() {
        return WebClient.builder()
                .baseUrl(lhRentalHouseListUrl)
                .build();
    }
}

