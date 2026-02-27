package io.github.stasbykov.datapreparer.internal.core;

import io.github.stasbykov.datapreparer.api.core.Fixture;
import io.github.stasbykov.datapreparer.api.core.FixtureRegistry;
import io.github.stasbykov.datapreparer.api.core.FixtureTemplate;
import io.github.stasbykov.datapreparer.internal.util.scanner.ClassScanner;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A fixture handler class that loads fixture templates from various registries.
 *
 * @see FixtureRegistry
 * @see FixtureTemplate
 * @since 1.0.0
 */
public class FixtureHandler {

    private final List<FixtureTemplate<? extends Fixture>> templates;

    /**
     * Parameter value for storing the package name with {@link FixtureRegistry}
     * Used to configure the packet scan scope limitation when creating a template registry.
     */
    private final static String PACKAGE_NAME_PROPERTIES = "fixture.package.registry";

    /**
     * A constructor for creating a fixture handler with a concrete implementation of an interface scanner.
     *
     * @param scanner Interface scanner for searching and instantiating classes
     * @throws NullPointerException If the scanner was not transmitted
     */
    public FixtureHandler(@NotNull ClassScanner scanner) {
        // Loading fixture registers
        @SuppressWarnings("unchecked ")
        Class<FixtureRegistry<? extends Fixture>> fixtureRegistryClass = (Class<FixtureRegistry<? extends Fixture>>) (Class<?>) FixtureRegistry.class;
        List<FixtureRegistry<? extends Fixture>> registries = scanner.findAndInstantiate(fixtureRegistryClass, getPackageName().orElse(""));
        // Adding fixture templates from registries to the list
        templates = List.copyOf(registries.stream()
                        .filter(Objects::nonNull)
                        .map(FixtureRegistry::getTemplates)
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .toList());

    }

    /**
     * Search for a fixture template by name.
     *
     * @param templateName The name of the fixture template to search for.
     * @return Optional fixture template value matching the given name.
     * @throws NullPointerException If fixture template name is null
     */
    public Optional<FixtureTemplate<? extends Fixture>> getTemplate(String templateName) {
        requireNonNull(templateName, "Template name can`t be null");

        return templates.stream()
                .filter(template -> templateName.equals(template.name()))
                .findFirst();
    }

    /**
     *  Returns the values specified in the properties (pom.xml, gradle.properties or -D) of a package with {@link FixtureRegistry }
     *
     * @return The value of the package with {@link FixtureRegistry }, or empty if not set
     */
    private Optional<String> getPackageName() {
        return Optional.ofNullable(System.getProperty(PACKAGE_NAME_PROPERTIES));
    }
}
