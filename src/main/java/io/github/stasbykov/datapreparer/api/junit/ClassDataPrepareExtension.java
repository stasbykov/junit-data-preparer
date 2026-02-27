package io.github.stasbykov.datapreparer.api.junit;

import io.github.stasbykov.datapreparer.api.annotation.ClassDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.FixtureInject;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.internal.junit.PrepareExtensionManager;
import io.github.stasbykov.datapreparer.internal.util.scanner.ClassgraphScanner;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static io.github.stasbykov.datapreparer.internal.util.junit.ContextUtils.getRequiredAnnotation;

/**
 * A JUnit extension that allows you to prepare fixtures before all tests in a class.
 *
 * @since 1.0.0
 */
public final class ClassDataPrepareExtension implements TestInstancePostProcessor {

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
        logger.info("Starting of the method of preparing fixtures before testing.");
        FixtureBatchCollection fixtureBatches = prepareExtensionManager.computeValueOnce(context);

        if (!getRequiredAnnotation(context, ClassDataSetup.class).inject()) {
            logger.info("Saving fixtures to the field is disabled. Skipping step.");
            return;
        }
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
        Field field = findFieldWithAnnotation(testInstance.getClass(), FixtureInject.class)
                .orElseThrow(() -> new IllegalArgumentException("Fixture saving failed because a field annotated with @FixtureInject and of type FixtureBatchCollection was not found. Check the annotation and field type."));
        field.setAccessible(true);
        field.set(testInstance, fixtureBatches);
    }

    /**
     * Finds the first field in a class that matches the given type and the presence of an annotation.
     *
     * @param clazz the class to search for the field
     * @param annotationClass the class of the annotation the field should contain
     * @return Optional field that satisfies the conditions
     */
    private Optional<Field> findFieldWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.getType() == FixtureBatchCollection.class && field.isAnnotationPresent(annotationClass))
                .findFirst();
    }
}
