package com.projfiftyk.apicore.services.httptemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projfiftyk.apicore.domain.httptemplate.HeaderEntry;
import com.projfiftyk.apicore.domain.httptemplate.HttpTemplate;
import com.projfiftyk.apicore.engine.http.HttpMethod;
import com.projfiftyk.apicore.engine.http.HttpTemplateConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for the seam described in architecture.md §11: mapping the persisted
 * (Spring-flavored) entity into the engine's plain-Java config.
 */
class HttpTemplateEngineMapperTest {

    private final HttpTemplateEngineMapper mapper = new HttpTemplateEngineMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsEveryFieldOntoTheEngineConfig() {
        HttpTemplate template = new HttpTemplate();
        template.setMethod(HttpMethod.POST);
        template.setUrlTemplate("https://example.com/{{id}}");
        template.setHeaderTemplate(List.of(new HeaderEntry("Authorization", "Bearer {{token}}", true)));
        ObjectNode body = objectMapper.createObjectNode().put("name", "{{name}}");
        template.setBodyTemplate(body);

        HttpTemplateConfig config = mapper.toEngineConfig(template);

        assertThat(config.method()).isEqualTo(HttpMethod.POST);
        assertThat(config.urlTemplate()).isEqualTo("https://example.com/{{id}}");
        assertThat(config.headerTemplate()).hasSize(1);
        assertThat(config.headerTemplate().get(0).name()).isEqualTo("Authorization");
        assertThat(config.headerTemplate().get(0).secret()).isTrue();
        assertThat(config.bodyTemplate()).isEqualTo(body);
    }

    @Test
    void nullHeaderTemplateBecomesAnEmptyList() {
        HttpTemplate template = new HttpTemplate();
        template.setMethod(HttpMethod.GET);
        template.setUrlTemplate("https://example.com");

        HttpTemplateConfig config = mapper.toEngineConfig(template);

        assertThat(config.headerTemplate()).isEmpty();
    }
}
