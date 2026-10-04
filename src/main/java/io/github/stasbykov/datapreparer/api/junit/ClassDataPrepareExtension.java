package io.github.stasbykov.datapreparer.api.junit;

import io.github.stasbykov.datapreparer.api.annotation.ClassDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.FixtureInject;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.internal.junit.PrepareExtensionManager;
import io.github.stasbykov.datapreparer.internal.util.scanner.ClassgraphScanner;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static io.github.stasbykov.datapreparer.internal.util.junit.ContextUtils.getRequiredAnnotation;

/**
 * A JUnit extension that allows you to prepare fixtures before all tests in a class.
 *
 * @since 1.0.0
 */
public final class ClassDataPrepareExtension implements BeforeAllCallback, TestInstancePostProcessor {

    private final PrepareExtensionManager prepareExtensionManager;
    private final Logger logger;

    /**
     * A default constructor that initializes the object using Classgraph to scan fixture interfaces.
     */
    public ClassDataPrepareExtension() {
        this(new PrepareExtensionManager(new ClassgraphScanner(),
                ExtensionContext.Namespace.create(ClassDataPrepareExtension.class)),
                LoggerFactory.getLogger(ClassDataPrepareExtension.class));
    }

    /**
     * Loads class fixtures before {@code @BeforeAll} methods are invoked.
     *
     * @param context JUnit extension context
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        logger.info("Starting fixture preparation before all tests in the class.");
        prepareExtensionManager.computeValueOnce(context);
    }

    /**
     * Package-private constructor for tests
     *
     */
    private ClassDataPrepareExtension(PrepareExtensionManager prepareExtensionManager, Logger logger) {
        this.prepareExtensionManager = prepareExtensionManager;
        this.logger = logger;

    }

    /**
     * Prepares fixtures according to templates and saves the prepared fixtures in a field annotated with @FixtureInject.
     * A method called after an instance of the test class is created.
     *
     * @param testInstance test class instance
     * @param context JUnit extension context
     * @throws IllegalAccessException if errors occur when working with class fields
     */
    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws IllegalAccessException {
        if (!getRequiredAnnotation(context, ClassDataSetup.class).inject()) {
            logger.info("Saving fixtures to the field is disabled. Skipping step.");
            return;
        }
        FixtureBatchCollection fixtureBatches = prepareExtensionManager.computeValueOnce(context);
        logger.info("Saving fixtures to a field annotated with @FixtureInject.");
        injectLoadedFixtures(testInstance, fixtureBatches);
    }


    /**
     * Injects prepared fixtures into a field annotated with @FixtureInject.
     *
     * @param testInstance test class instance
     * @param fixtureBatches prepared fixtures
     * @throws IllegalAccessException if errors occur when working with class fields
     */
    private void injectLoadedFixtures(Object testInstance, FixtureBatchCollection fixtureBatches) throws IllegalAccessException {
        requireNonNull(fixtureBatches, "Saving fixtures could not be completed - no fixtures were found.");
        Field field = findInjectionField(testInstance.getClass());
        field.setAccessible(true);
        field.set(testInstance, fixtureBatches);
    }

    /**
     * Finds a single field annotated with {@link FixtureInject} in the complete class hierarchy.
     *
     * @param clazz the class to search for the field
     * @return field that satisfies the injection contract
     */
    private Field findInjectionField(Class<?> clazz) {
        List<Field> annotatedFields = new ArrayList<>();
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            Arrays.stream(current.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(FixtureInject.class))
                    .forEach(annotatedFields::add);
        }

        if (annotatedFields.size() != 1) {
            throw new IllegalArgumentException(
                    "Fixture saving requires exactly one field annotated with @FixtureInject, but found: "
                            + annotatedFields.size());
        }

        Field field = annotatedFields.get(0);
        if (field.getType() != FixtureBatchCollection.class) {
            throw new IllegalArgumentException("Fixture saving failed because the field annotated with "
                    + "@FixtureInject must be of type FixtureBatchCollection.");
        }
        return field;
    }
}
