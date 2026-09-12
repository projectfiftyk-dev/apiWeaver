package com.projfiftyk.apicore.domain.httptemplate;

import com.projfiftyk.apicore.domain.common.AbstractJsonListConverter;
import jakarta.persistence.Converter;

@Converter
public class HeaderEntryListConverter extends AbstractJsonListConverter<HeaderEntry> {

    public HeaderEntryListConverter() {
        super(HeaderEntry.class);
    }
}
