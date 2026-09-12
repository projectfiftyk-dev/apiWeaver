package com.projfiftyk.apicore.services.httptemplate;

import com.projfiftyk.apicore.domain.httptemplate.HttpTemplate;
import com.projfiftyk.apicore.engine.http.HeaderConfig;
import com.projfiftyk.apicore.engine.http.HttpTemplateConfig;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The seam described in architecture.md §11: maps the Spring-flavored persisted
 * {@code HttpTemplate} entity into the framework-free {@code HttpTemplateConfig} the
 * Engine's Factory consumes. Nothing on the engine side ever sees the JPA entity.
 */
@Component
public class HttpTemplateEngineMapper {

    public HttpTemplateConfig toEngineConfig(HttpTemplate template) {
        List<HeaderConfig> headers = template.getHeaderTemplate() == null
                ? List.of()
                : template.getHeaderTemplate().stream()
                .map(h -> new HeaderConfig(h.name(), h.value(), h.secret()))
                .toList();

        return new HttpTemplateConfig(
                template.getMethod(),
                template.getUrlTemplate(),
                headers,
                template.getBodyTemplate()
        );
    }
}
