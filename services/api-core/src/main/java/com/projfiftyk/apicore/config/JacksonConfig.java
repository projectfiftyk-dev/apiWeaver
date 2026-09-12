package com.projfiftyk.apicore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring Boot 4 defaults to Jackson 3 (tools.jackson.*), but the engine's expression
 * resolution depends on io.burt:jmespath-jackson, which only supports Jackson 2
 * (com.fasterxml.jackson.*). Rather than run two Jackson major versions side by side,
 * this app standardizes on Jackson 2 everywhere JSON is touched, and wires it as the
 * primary Spring MVC converter here.
 */
@Configuration
public class JacksonConfig implements WebMvcConfigurer {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper()));
    }
}
