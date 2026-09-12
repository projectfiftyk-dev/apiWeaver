package com.projfiftyk.apicore.web.httptemplate;

import com.projfiftyk.apicore.services.httptemplate.HttpTemplateService;
import com.projfiftyk.apicore.transfer.httptemplate.request.HttpTemplateRequest;
import com.projfiftyk.apicore.transfer.httptemplate.response.HttpTemplateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/http-templates")
public class HttpTemplateController {

    private final HttpTemplateService service;

    public HttpTemplateController(HttpTemplateService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HttpTemplateResponse create(@Valid @RequestBody HttpTemplateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<HttpTemplateResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public HttpTemplateResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public HttpTemplateResponse update(@PathVariable Long id, @Valid @RequestBody HttpTemplateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
