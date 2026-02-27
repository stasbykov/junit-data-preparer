package io.github.stasbykov.datapreparer.api.annotation;

import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.api.junit.MethodDataPrepareExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Annotation used to set up method-level test data.
 * <p>
 * This annotation is intended to be applied to parameters in JUnit 5 test methods.
 * It specifies one or more {@link Template} annotations that define how test data should be prepared
 * before the execution of the annotated test method.
 * </p>
 * <p>
 * The annotation triggers the {@link MethodDataPrepareExtension} to process the provided templates
 * and prepare the necessary data context for the test.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * class UserServiceTest {
 *
 *     @Test
 *     void someTest(@MethodDataSetup({
 *             @Template(name = "first_template", count = 2),
 *             @Template(name = "second_template", count = 10)} FixtureBatchCollection loadedFixtures) {
 *             // tests...
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Note:</strong> Annotation is supported only before method argument.
 *
 * @see FixtureBatchCollection
 * @see Template
 * @see MethodDataPrepareExtension
 * @since 1.0
 */
@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MethodDataPrepareExtension.class)
public @interface MethodDataSetup {
     Template[] value();
}
