package io.github.stasbykov.datapreparer.internal.junit;

import io.github.stasbykov.datapreparer.api.annotation.ClassDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.MethodDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.Template;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.internal.core.FixtureHandler;
import io.github.stasbykov.datapreparer.internal.util.scanner.ClassScanner;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static io.github.stasbykov.datapreparer.internal.util.junit.ContextUtils.getAnnotation;

/**
 * Defines the basic logic for working with fixtures
 *
 * @see FixtureHandler
 * @see TestDataPreparer
 * @since 1.0.0
 */
public final class PrepareExtensionManager {
    private final TestDataPreparer testDataPreparer;
    private final ExtensionContext.Namespace namespace;
    private final String LOADED_FIXTURES_KEY = "loadedFixtures";

    public PrepareExtensionManager(ClassScanner scanner, ExtensionContext.Namespace namespace) {
        requireNonNull(scanner);
        FixtureHandler handler = new FixtureHandler(scanner);
        this.testDataPreparer = new TestDataPreparer(handler);
        this.namespace = requireNonNull(namespace);
    }

    /**
     * Computes the fixture value once and stores it in the context storage.
     *
     * @param context JUnit extension context
     * @return prepared fixtures
     */
    public FixtureBatchCollection computeValueOnce(ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(namespace);
        return store.getOrComputeIfAbsent(LOADED_FIXTURES_KEY, key -> prepareData(context), FixtureBatchCollection.class);
    }

    /**
     * Calculates the parameter value once and stores it in the extension's storage.
     *
     * @param parameterContext JUnit parameter context
     * @param extensionContext JUnit extension context
     * @return prepared fixture data as {@link FixtureBatchCollection}
     */
    public FixtureBatchCollection computeValueOnce(ParameterContext parameterContext, ExtensionContext extensionContext) {
        ExtensionContext.Store store = extensionContext.getStore(namespace);
        return store.getOrComputeIfAbsent(LOADED_FIXTURES_KEY, key -> prepareData(parameterContext), FixtureBatchCollection.class);
    }

    /**
     * Prepares fixture data based on templates.
     *
     * @param context JUnit extension context
     * @return wrapper around prepared fixtures
     */
    private FixtureBatchCollection prepareData(ExtensionContext context) {
        return getTemplates(context)
                .stream()
                .map(testDataPreparer::processTemplatesForLoading)
                .flatMap(Collection::stream)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        FixtureBatchCollection::new
                ));
    }

    /**
     * Prepares fixture data based on the templates specified in the {@link MethodDataSetup} annotation.
     *
     * @param parameterContext JUnit parameter context
     * @return prepared fixture data as {@link FixtureBatchCollection}
     */
    private FixtureBatchCollection prepareData(ParameterContext parameterContext) {
        return getTemplates(parameterContext)
                .stream()
                .map(testDataPreparer::processTemplatesForLoading)
                .flatMap(Collection::stream)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        FixtureBatchCollection::new
                ));
    }

    /**
     * Gets an array of {@link Template} annotations from the method context.
     *
     * @param parameterContext JUnit parameter context
     * @return optional array of {@link Template} annotations
     */
    private Optional<Template[]> getTemplates(ParameterContext parameterContext) {
        return parameterContext.findAnnotation(MethodDataSetup.class).map(MethodDataSetup::value);
    }

    /**
     * Gets an array of {@link Template} annotations from the extension context.
     *
     * @param extensionContext JUnit extension context
     * @return optional array of {@link Template} annotations
     */
    private Optional<Template[]> getTemplates(ExtensionContext extensionContext) {
        return getAnnotation(extensionContext, ClassDataSetup.class).map(ClassDataSetup::value);
    }
}
