package com.projfiftyk.apicore.services.chain;

import com.projfiftyk.apicore.domain.chain.Chain;
import com.projfiftyk.apicore.domain.chain.ChainStep;
import com.projfiftyk.apicore.repository.chain.ChainRepository;
import com.projfiftyk.apicore.services.common.NotFoundException;
import com.projfiftyk.apicore.transfer.chain.request.ChainRequest;
import com.projfiftyk.apicore.transfer.chain.request.ChainStepRequest;
import com.projfiftyk.apicore.transfer.chain.response.ChainResponse;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChainServiceImplTest {

    @Mock
    private ChainRepository repository;

    @InjectMocks
    private ChainServiceImpl service;

    @Test
    void createSavesTheStepsInTheGivenOrder() {
        when(repository.save(any(Chain.class))).thenAnswer(invocation -> {
            Chain saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ChainRequest request = new ChainRequest("Get then post", null, List.of(
                new ChainStepRequest(10L, 0),
                new ChainStepRequest(20L, 1)
        ));

        ChainResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.steps()).hasSize(2);
        assertThat(response.steps().get(0).templateId()).isEqualTo(10L);
        assertThat(response.steps().get(1).templateId()).isEqualTo(20L);
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateReplacesNameDescriptionAndSteps() {
        Chain existing = new Chain();
        existing.setId(5L);
        existing.setName("Old");
        existing.setSteps(List.of(new ChainStep(1L, 0)));

        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Chain.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChainRequest request = new ChainRequest("New", "updated", List.of(new ChainStepRequest(2L, 0)));

        ChainResponse response = service.update(5L, request);

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.description()).isEqualTo("updated");
        assertThat(response.steps()).extracting(s -> s.templateId()).containsExactly(2L);
    }

    @Test
    void deleteThrowsNotFoundAndNeverDeletesWhenMissing() {
        when(repository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteRemovesAnExistingChain() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }
}
