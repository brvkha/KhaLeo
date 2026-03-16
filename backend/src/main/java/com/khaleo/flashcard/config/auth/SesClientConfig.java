package com.khaleo.flashcard.config.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class SesClientConfig {

    @Bean
    SesClient sesClient(@Value("${app.dynamo.region:us-east-1}") String region) {
        return SesClient.builder().region(Region.of(region)).build();
    }
}
