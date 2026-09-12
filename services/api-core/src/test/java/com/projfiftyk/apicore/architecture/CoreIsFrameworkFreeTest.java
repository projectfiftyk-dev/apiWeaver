package com.projfiftyk.apicore.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces architecture.md §10: the engine package may depend on plain Java and
 * framework-agnostic libraries only — never on org.springframework.*.
 */
class CoreIsFrameworkFreeTest {

    @Test
    void engineNeverDependsOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.projfiftyk.apicore.engine..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..");

        rule.check(new ClassFileImporter().importPackages("com.projfiftyk.apicore"));
    }
}
