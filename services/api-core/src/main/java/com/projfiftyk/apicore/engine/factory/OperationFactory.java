package com.projfiftyk.apicore.engine.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projfiftyk.apicore.engine.Operation;
import com.projfiftyk.apicore.engine.expression.ExpressionResolver;
import com.projfiftyk.apicore.engine.http.HttpOperation;
import com.projfiftyk.apicore.engine.http.HttpTemplateConfig;

import java.net.http.HttpClient;

/**
 * Factory Method: turns a Template's config into a live, runnable Operation.
 * The seam between the CRUD side (saving/editing Templates) and the execution side
 * (the Engine running Operations) — CRUD never touches behavior.
 */
public class OperationFactory {

    private final ExpressionResolver expressionResolver;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OperationFactory(ExpressionResolver expressionResolver, HttpClient httpClient, ObjectMapper objectMapper) {
        this.expressionResolver = expressionResolver;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public Operation create(HttpTemplateConfig config) {
        return new HttpOperation(config, expressionResolver, httpClient, objectMapper);
    }
}
