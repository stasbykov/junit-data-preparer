package io.github.stasbykov.datapreparer.api.annotation;

import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.api.junit.ClassDataPrepareExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Annotation to trigger test data generation at the class level in JUnit 5 tests.
 *
 * <p>When this annotation is applied to a test class, it activates the data preparation mechanism
 * via {@link ClassDataPrepareExtension}, which processes the specified templates and prepares
 * fixture data before test execution.</p>
 *
 * <p>The annotated class can define one or more {@link Template} configurations that describe
 * how test data should be generated or loaded. Additionally, the {@code inject} flag controls
 * whether the prepared data should be automatically injected into the test instance.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @ClassDataSetup(value = {
 *     @Template(name = "first_template", count = 2),
 *     @Template(name = "second_template", count = 10)},
 *     inject = true)
 * class UserServiceTest {
 *
 *     @FixtureInject
 *     private FixtureBatchCollection loadedFixtures;
 *
 *     @Test
 *     void someTest() {
 *         // use loadedFixtures
 *     }
 *     // tests...
 * }
 * }</pre>
 *
 * <p><strong>Note:</strong> Annotation is supported only before class.
 *
 * @see FixtureBatchCollection
 * @see Template
 * @see ClassDataPrepareExtension
 * @since 1.0
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ClassDataPrepareExtension.class)
public @interface ClassDataSetup {
    Template[] value();
    boolean inject() default false;
}
