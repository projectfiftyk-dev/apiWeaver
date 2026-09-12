package com.projfiftyk.apicore.services.httptemplate;

import com.projfiftyk.apicore.transfer.httptemplate.request.HttpTemplateRequest;
import com.projfiftyk.apicore.transfer.httptemplate.response.HttpTemplateResponse;

import java.util.List;

public interface HttpTemplateService {

    HttpTemplateResponse create(HttpTemplateRequest request);

    List<HttpTemplateResponse> findAll();

    HttpTemplateResponse findById(Long id);

    HttpTemplateResponse update(Long id, HttpTemplateRequest request);

    void delete(Long id);
}
