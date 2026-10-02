package io.github.stasbykov.datapreparer.api.annotation;

import io.github.stasbykov.datapreparer.api.junit.FixtureArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Supplies loaded fixtures to a JUnit parameterized test.
 * Each template forms one argument axis; test invocations are the Cartesian
 * product of all axes in declaration order.
 *
 * @since 2.0.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(FixtureArgumentsProvider.class)
public @interface FixtureSource {

    /**
     * Templates forming the argument axes in test-method parameter order.
     *
     * @return fixture templates
     */
    Template[] value();
}
