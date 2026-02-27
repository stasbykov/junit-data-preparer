package io.github.stasbykov.datapreparer.api.annotation;

import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;

import java.lang.annotation.*;

/**
 * Marks the field into which the generated test data should be written.
 * <p>
 * This annotation is used to designate a field that contains
 * test data generation. The annotated class must contain
 * a single field of type {@link io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection} which defines the batch of fixtures
 * to be created.
 * </p>
 *
 * <p>The framework will scan for this annotation at runtime and process the
 * associated {@code FixtureBatchCollection} field to inject test data.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * public class UserTestFixture {
 *
 *     @FixtureInject
 *     private FixtureBatchCollection loadedFixtures;
 *
 *     // Other configuration methods or fields can be added here
 * }
 * }</pre>
 *
 * <p><strong>Note:</strong> Only one {@code @FixtureInject} annotation per class is allowed,
 * and the class must contain exactly one field of type {@link io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection}.</p>
 *
 * @see FixtureBatchCollection
 * @since 1.0
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FixtureInject {
}
