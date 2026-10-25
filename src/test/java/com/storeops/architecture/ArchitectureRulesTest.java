package com.storeops.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.storeops.common.error.AppError;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Deterministic hard gates for the harness Evaluator. Run alone with
 * {@code mvn test -Dtest=ArchitectureRulesTest}. Each test is one gate (G1 to G5).
 */
class ArchitectureRulesTest {

    private static final List<String> MODULES = List.of("activities", "programmes", "staff", "alerts", "reports");

    private static final JavaClasses MAIN = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.storeops");

    private static DescribedPredicate<JavaClass> internalsOfOtherModules(String module) {
        String[] others = MODULES.stream().filter(m -> !m.equals(module))
                .map(m -> "com.storeops." + m + "..").toArray(String[]::new);
        return resideInAnyPackage(others).and(simpleNameEndingWith("Repository")
                .or(simpleNameEndingWith("Service")).or(simpleNameEndingWith("Controller")));
    }

    /** G1: a module reaches another module only through its Reader interface, never its repository or service. */
    @Test
    void g1ModulesDoNotImportOtherModulesInternals() {
        for (String module : MODULES) {
            noClasses().that().resideInAPackage("com.storeops." + module + "..")
                    .should().dependOnClassesThat(internalsOfOtherModules(module))
                    .as("G1: " + module + " must not depend on another module's Repository/Service/Controller")
                    .check(MAIN);
        }
    }

    /** G1: no circular module dependencies. */
    @Test
    void g1NoCyclesBetweenModules() {
        slices().matching("com.storeops.(*)..").should().beFreeOfCycles().check(MAIN);
    }

    /** G2: the only Throwables the app constructs are AppError subclasses. */
    @Test
    void g2OnlyAppErrorsAreConstructed() {
        noClasses().that().resideOutsideOfPackage("com.storeops.common.error..")
                .should().callConstructorWhere(describe("constructs a Throwable that is not an AppError",
                        call -> call.getTargetOwner().isAssignableTo(Throwable.class)
                                && !call.getTargetOwner().isAssignableTo(AppError.class)))
                .as("G2: throw only AppError subclasses (NotFoundError, ConflictError, ValidationError, ...)")
                .check(MAIN);
    }

    /** G3: only EventBus publishes events. */
    @Test
    void g3OnlyEventBusPublishesEvents() {
        noClasses().that().resideOutsideOfPackage("com.storeops.common.event..")
                .should().callMethodWhere(describe("calls ApplicationEventPublisher.publishEvent",
                        call -> call.getTargetOwner().isAssignableTo(ApplicationEventPublisher.class)
                                && "publishEvent".equals(call.getName())))
                .as("G3: publish through common.event.EventBus, never ApplicationEventPublisher directly")
                .check(MAIN);
    }

    /** G4: Controller -> Service -> Repository only. */
    @Test
    void g4LayersAreSeparated() {
        noClasses().that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                .as("G4: controllers must go through a Service, never a Repository").check(MAIN);
        noClasses().that().haveSimpleNameEndingWith("Repository")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Service")
                .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .as("G4: repositories must not depend on services or controllers").check(MAIN);
        noClasses().that().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
                .as("G4: services must not depend on controllers").check(MAIN);
    }

    /** G5: reports calls other modules only through their Reader interfaces. */
    @Test
    void g5ReportsOnlyCallReaders() {
        String[] others = MODULES.stream().filter(m -> !m.equals("reports"))
                .map(m -> "com.storeops." + m + "..").toArray(String[]::new);
        noClasses().that().resideInAPackage("com.storeops.reports..")
                .should().callMethodWhere(describe("calls a non-Reader method of another module",
                        call -> resideInAnyPackage(others).test(call.getTargetOwner())
                                && !call.getTargetOwner().getSimpleName().endsWith("Reader")
                                && !call.getTargetOwner().isRecord()))
                .as("G5: reports is read-only; call other modules only via *Reader interfaces")
                .check(MAIN);
    }
}
