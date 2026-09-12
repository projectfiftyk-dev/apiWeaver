package com.projfiftyk.apicore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projfiftyk.apicore.engine.Engine;
import com.projfiftyk.apicore.engine.expression.ExpressionResolver;
import com.projfiftyk.apicore.engine.expression.JmesPathExpressionResolver;
import com.projfiftyk.apicore.engine.factory.OperationFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

/**
 * Wires the framework-free engine package as Spring beans. This is the only place
 * allowed to import both {@code org.springframework.*} and {@code engine.*}.
 */
@Configuration
public class EngineConfig {

    @Bean
    public ExpressionResolver expressionResolver() {
        return new JmesPathExpressionResolver();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    public OperationFactory operationFactory(ExpressionResolver expressionResolver, HttpClient httpClient,
                                              ObjectMapper objectMapper) {
        return new OperationFactory(expressionResolver, httpClient, objectMapper);
    }

    @Bean
    public Engine engine() {
        return new Engine();
    }
}
