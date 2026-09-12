package com.projfiftyk.apicore.behavior;

import com.projfiftyk.apicore.domain.httptemplate.HttpTemplate;
import com.projfiftyk.apicore.engine.http.HttpMethod;
import com.projfiftyk.apicore.repository.httptemplate.HttpTemplateRepository;
import com.projfiftyk.apicore.services.common.NotFoundException;
import com.projfiftyk.apicore.services.httptemplate.HttpTemplateServiceImpl;
import com.projfiftyk.apicore.transfer.httptemplate.request.HeaderEntryRequest;
import com.projfiftyk.apicore.transfer.httptemplate.request.HttpTemplateRequest;
import com.projfiftyk.apicore.transfer.httptemplate.response.HttpTemplateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * A living spec (Given/When/Then) for the HttpTemplate CRUD lifecycle rules that
 * aren't obvious from the entity shape alone: versioning and secret masking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("An HttpTemplate's lifecycle")
class HttpTemplateLifecycleBehaviorTest {

    @Mock
    private HttpTemplateRepository repository;

    @InjectMocks
    private HttpTemplateServiceImpl service;

    private HttpTemplateRequest requestWithHeader(HeaderEntryRequest header) {
        return new HttpTemplateRequest("Create product", null, HttpMethod.POST,
                "https://example.com", List.of(header), null, null);
    }

    @Nested
    @DisplayName("given a brand new template")
    class GivenABrandNewTemplate {

        @Test
        @DisplayName("when created, then its version starts at 1")
        void versionStartsAtOne() {
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            HttpTemplateResponse response = service.create(requestWithHeader(
                    new HeaderEntryRequest("Accept", "application/json", false)));

            assertThat(response.version()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("given an existing template at version 3")
    class GivenAnExistingTemplateAtVersionThree {

        private HttpTemplate existing() {
            HttpTemplate template = new HttpTemplate();
            template.setId(1L);
            template.setVersion(3);
            template.setName("Old");
            template.setMethod(HttpMethod.GET);
            template.setUrlTemplate("https://old.example.com");
            return template;
        }

        @Test
        @DisplayName("when updated, then its version becomes 4, never reset to 1")
        void versionIncrementsRatherThanReset() {
            when(repository.findById(1L)).thenReturn(Optional.of(existing()));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            HttpTemplateResponse response = service.update(1L, requestWithHeader(
                    new HeaderEntryRequest("Accept", "application/json", false)));

            assertThat(response.version()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("given a header marked secret")
    class GivenASecretHeader {

        @Test
        @DisplayName("when the template is created, then the response carries the flag but masks the value")
        void secretValueIsMaskedOnCreate() {
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            HttpTemplateResponse response = service.create(requestWithHeader(
                    new HeaderEntryRequest("Authorization", "Bearer super-secret", true)));

            assertThat(response.headerTemplate().get(0).secret()).isTrue();
            assertThat(response.headerTemplate().get(0).value()).isNull();
        }

        @Test
        @DisplayName("when a non-secret header is created, then its value is returned as-is")
        void nonSecretValueIsReturnedVerbatim() {
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            HttpTemplateResponse response = service.create(requestWithHeader(
                    new HeaderEntryRequest("Accept", "application/json", false)));

            assertThat(response.headerTemplate().get(0).value()).isEqualTo("application/json");
        }
    }

    @Nested
    @DisplayName("given no template exists with a given id")
    class GivenNoTemplateExists {

        @Test
        @DisplayName("when looked up, updated, or deleted, then each operation fails predictably instead of returning null")
        void everyOperationFailsPredictably() {
            when(repository.findById(404L)).thenReturn(Optional.empty());
            when(repository.existsById(404L)).thenReturn(false);

            assertThatThrownBy(() -> service.findById(404L)).isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> service.update(404L, requestWithHeader(
                    new HeaderEntryRequest("Accept", "application/json", false))))
                    .isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> service.delete(404L)).isInstanceOf(NotFoundException.class);
        }
    }
}
