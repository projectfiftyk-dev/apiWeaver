package com.projfiftyk.apicore.domain.httptemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.projfiftyk.apicore.domain.common.JsonNodeConverter;
import com.projfiftyk.apicore.engine.http.HttpMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.util.List;

/**
 * The persisted, named, editable config for one HTTP operation. This JPA entity is a
 * separate, Spring-flavored representation from {@code engine.http.HttpTemplateConfig} —
 * see architecture.md §11 on where the two are mapped into each other.
 */
@Entity
public class HttpTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private int version;

    @Enumerated(EnumType.STRING)
    private HttpMethod method;

    @Column(length = 2048)
    private String urlTemplate;

    @Lob
    @Convert(converter = HeaderEntryListConverter.class)
    private List<HeaderEntry> headerTemplate;

    @Lob
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode bodyTemplate;

    @Lob
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode declaredOutput;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public List<HeaderEntry> getHeaderTemplate() {
        return headerTemplate;
    }

    public void setHeaderTemplate(List<HeaderEntry> headerTemplate) {
        this.headerTemplate = headerTemplate;
    }

    public JsonNode getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(JsonNode bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public JsonNode getDeclaredOutput() {
        return declaredOutput;
    }

    public void setDeclaredOutput(JsonNode declaredOutput) {
        this.declaredOutput = declaredOutput;
    }
}
