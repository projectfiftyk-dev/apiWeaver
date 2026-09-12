package com.projfiftyk.apicore.engine.expression;

import com.fasterxml.jackson.databind.JsonNode;
import io.burt.jmespath.Expression;
import io.burt.jmespath.JmesPath;
import io.burt.jmespath.jackson.JacksonRuntime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JmesPathExpressionResolver implements ExpressionResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(.+?)\\s*}}");

    private final JmesPath<JsonNode> jmesPath = new JacksonRuntime();

    @Override
    public String resolve(String template, JsonNode payload) {
        if (template == null) {
            return null;
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String resolved = evaluate(matcher.group(1), payload);
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String evaluate(String expression, JsonNode payload) {
        Expression<JsonNode> compiled = jmesPath.compile(expression);
        JsonNode value = compiled.search(payload);

        if (value == null || value.isNull()) {
            return "";
        }
        return value.isTextual() ? value.asText() : value.toString();
    }
}
