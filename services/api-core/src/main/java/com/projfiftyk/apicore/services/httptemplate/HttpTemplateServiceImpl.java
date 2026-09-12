package com.projfiftyk.apicore.services.httptemplate;

import com.projfiftyk.apicore.domain.httptemplate.HeaderEntry;
import com.projfiftyk.apicore.domain.httptemplate.HttpTemplate;
import com.projfiftyk.apicore.repository.httptemplate.HttpTemplateRepository;
import com.projfiftyk.apicore.services.common.NotFoundException;
import com.projfiftyk.apicore.transfer.httptemplate.request.HeaderEntryRequest;
import com.projfiftyk.apicore.transfer.httptemplate.request.HttpTemplateRequest;
import com.projfiftyk.apicore.transfer.httptemplate.response.HeaderEntryResponse;
import com.projfiftyk.apicore.transfer.httptemplate.response.HttpTemplateResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HttpTemplateServiceImpl implements HttpTemplateService {

    private final HttpTemplateRepository repository;

    public HttpTemplateServiceImpl(HttpTemplateRepository repository) {
        this.repository = repository;
    }

    @Override
    public HttpTemplateResponse create(HttpTemplateRequest request) {
        HttpTemplate template = new HttpTemplate();
        template.setVersion(1);
        applyRequest(template, request);
        return toResponse(repository.save(template));
    }

    @Override
    public List<HttpTemplateResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public HttpTemplateResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Override
    public HttpTemplateResponse update(Long id, HttpTemplateRequest request) {
        HttpTemplate template = getOrThrow(id);
        template.setVersion(template.getVersion() + 1);
        applyRequest(template, request);
        return toResponse(repository.save(template));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("HttpTemplate not found: " + id);
        }
        repository.deleteById(id);
    }

    private HttpTemplate getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("HttpTemplate not found: " + id));
    }

    private void applyRequest(HttpTemplate template, HttpTemplateRequest request) {
        template.setName(request.name());
        template.setDescription(request.description());
        template.setMethod(request.method());
        template.setUrlTemplate(request.urlTemplate());
        template.setBodyTemplate(request.bodyTemplate());
        template.setDeclaredOutput(request.declaredOutput());

        List<HeaderEntry> headers = request.headerTemplate() == null
                ? List.of()
                : request.headerTemplate().stream()
                .map(h -> new HeaderEntry(h.name(), h.value(), h.secret()))
                .toList();
        template.setHeaderTemplate(headers);
    }

    private HttpTemplateResponse toResponse(HttpTemplate template) {
        List<HeaderEntryResponse> headers = template.getHeaderTemplate() == null
                ? List.of()
                : template.getHeaderTemplate().stream()
                .map(h -> new HeaderEntryResponse(h.name(), h.secret() ? null : h.value(), h.secret()))
                .toList();

        return new HttpTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getVersion(),
                template.getMethod(),
                template.getUrlTemplate(),
                headers,
                template.getBodyTemplate(),
                template.getDeclaredOutput()
        );
    }
}
