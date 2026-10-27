package no.hvl.dat250.lms.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "no.hvl.dat250.lms", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    private static final String ROOT = "no.hvl.dat250.lms.";

    @ArchTest
    static final ArchRule controllersShouldNotAccessRepositories = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because("controllers should access business logic through services");

    @ArchTest
    static final ArchRule servicesShouldNotAccessControllers = noClasses()
            .that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
            .because("services are called by controllers and other services");

    @ArchTest
    static final ArchRule domainShouldNotDependOnApplicationLayers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..api..", "..service..", "..repository..", "..config..", "..auth..")
            .because("domain types should be independent of HTTP handling and application orchestration");

    @ArchTest
    static final ArchRule repositoriesShouldNotAccessServicesOrControllers = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Service")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
            .because("persistence must not call back into services or HTTP controllers");

    @ArchTest
    static void repositoriesShouldOnlyBeUsedWithinTheirFeature(JavaClasses classes) {
        // Cross-feature collaboration goes through services, using ordinary Java calls.
        // Include auth explicitly: authentication should use the user service rather
        // than bypassing it to access the user repository.
        for (String feature : new String[] {
                "auth", "user", "course", "module", "content", "submission", "enrollment", "group"
        }) {
            noClasses()
                    .that().resideOutsideOfPackage(ROOT + feature + "..")
                    .should().dependOnClassesThat().resideInAPackage(ROOT + feature + ".repository..")
                    .because("other features should use " + feature + " services instead of its repositories")
                    .check(classes);
        }
    }
}
