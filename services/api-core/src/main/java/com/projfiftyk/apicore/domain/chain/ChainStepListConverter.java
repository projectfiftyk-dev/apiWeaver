package com.projfiftyk.apicore.domain.chain;

import com.projfiftyk.apicore.domain.common.AbstractJsonListConverter;
import jakarta.persistence.Converter;

@Converter
public class ChainStepListConverter extends AbstractJsonListConverter<ChainStep> {

    public ChainStepListConverter() {
        super(ChainStep.class);
    }
}
