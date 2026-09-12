package com.projfiftyk.apicore.repository.httptemplate;

import com.projfiftyk.apicore.domain.httptemplate.HttpTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HttpTemplateRepository extends JpaRepository<HttpTemplate, Long> {
}
