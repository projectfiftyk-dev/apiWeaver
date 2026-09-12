package com.projfiftyk.apicore.domain.chain;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.util.List;

/** A named, ordered list of {@code HttpTemplate} references — what "run everything" actually runs. */
@Entity
public class Chain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Lob
    @Convert(converter = ChainStepListConverter.class)
    private List<ChainStep> steps;

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

    public List<ChainStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ChainStep> steps) {
        this.steps = steps;
    }
}
