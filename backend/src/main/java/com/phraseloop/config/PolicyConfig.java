package com.phraseloop.config;

import com.phraseloop.service.ReviewIntervalPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PolicyConfig {

    @Bean
    public ReviewIntervalPolicy reviewIntervalPolicy() {
        return new ReviewIntervalPolicy();
    }
}
