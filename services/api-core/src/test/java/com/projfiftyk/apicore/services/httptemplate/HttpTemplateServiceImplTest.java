package com.projfiftyk.apicore.services.httptemplate;

import com.projfiftyk.apicore.domain.httptemplate.HeaderEntry;
import com.projfiftyk.apicore.domain.httptemplate.HttpTemplate;
import com.projfiftyk.apicore.engine.http.HttpMethod;
import com.projfiftyk.apicore.repository.httptemplate.HttpTemplateRepository;
import com.projfiftyk.apicore.services.common.NotFoundException;
import com.projfiftyk.apicore.transfer.httptemplate.request.HeaderEntryRequest;
import com.projfiftyk.apicore.transfer.httptemplate.request.HttpTemplateRequest;
import com.projfiftyk.apicore.transfer.httptemplate.response.HttpTemplateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpTemplateServiceImplTest {

    @Mock
    private HttpTemplateRepository repository;

    @InjectMocks
    private HttpTemplateServiceImpl service;

    @Test
    void createSavesANewTemplateAtVersionOne() {
        when(repository.save(any(HttpTemplate.class))).thenAnswer(invocation -> {
            HttpTemplate saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        HttpTemplateRequest request = new HttpTemplateRequest(
                "Get product", "fetches a product", HttpMethod.GET,
                "https://example.com/{{id}}", List.of(new HeaderEntryRequest("Accept", "application/json", false)),
                null, null);

        HttpTemplateResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.name()).isEqualTo("Get product");
        assertThat(response.headerTemplate()).hasSize(1);
        assertThat(response.headerTemplate().get(0).value()).isEqualTo("application/json");
    }

    @Test
    void secretHeaderValuesAreMaskedInTheResponse() {
        when(repository.save(any(HttpTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HttpTemplateRequest request = new HttpTemplateRequest(
                "Create product", null, HttpMethod.POST, "https://example.com",
                List.of(new HeaderEntryRequest("Authorization", "Bearer secret-token", true)),
                null, null);

        HttpTemplateResponse response = service.create(request);

        assertThat(response.headerTemplate().get(0).secret()).isTrue();
        assertThat(response.headerTemplate().get(0).value()).isNull();
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(42L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void updateIncrementsVersionAndOverwritesFields() {
        HttpTemplate existing = new HttpTemplate();
        existing.setId(7L);
        existing.setVersion(1);
        existing.setName("Old name");
        existing.setMethod(HttpMethod.GET);
        existing.setUrlTemplate("https://old.example.com");
        existing.setHeaderTemplate(List.of(new HeaderEntry("X-Old", "old", false)));

        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any(HttpTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HttpTemplateRequest request = new HttpTemplateRequest(
                "New name", null, HttpMethod.POST, "https://new.example.com", List.of(), null, null);

        HttpTemplateResponse response = service.update(7L, request);

        assertThat(response.version()).isEqualTo(2);
        assertThat(response.name()).isEqualTo("New name");
        assertThat(response.method()).isEqualTo(HttpMethod.POST);
        assertThat(response.urlTemplate()).isEqualTo("https://new.example.com");
    }

    @Test
    void deleteRemovesAnExistingTemplate() {
        when(repository.existsById(3L)).thenReturn(true);

        service.delete(3L);

        verify(repository).deleteById(3L);
    }

    @Test
    void deleteThrowsNotFoundAndNeverCallsDeleteWhenMissing() {
        when(repository.existsById(3L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(NotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void findAllMapsEveryPersistedTemplate() {
        HttpTemplate a = new HttpTemplate();
        a.setId(1L);
        a.setName("A");
        HttpTemplate b = new HttpTemplate();
        b.setId(2L);
        b.setName("B");
        when(repository.findAll()).thenReturn(List.of(a, b));

        List<HttpTemplateResponse> responses = service.findAll();

        assertThat(responses).extracting(HttpTemplateResponse::name).containsExactly("A", "B");
    }

    @Test
    void createPersistsTheGivenBodyAndDeclaredOutputVerbatim() {
        ArgumentCaptor<HttpTemplate> captor = ArgumentCaptor.forClass(HttpTemplate.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var body = mapper.createObjectNode().put("name", "{{name}}");

        service.create(new HttpTemplateRequest("T", null, HttpMethod.POST, "https://example.com", null, body, null));

        assertThat(captor.getValue().getBodyTemplate()).isEqualTo(body);
    }
}
