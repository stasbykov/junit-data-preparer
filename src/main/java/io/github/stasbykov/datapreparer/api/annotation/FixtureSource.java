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
 * The configured {@link #mode()} determines whether fixtures are supplied one at a time
 * or as the Cartesian product of template axes.
 * Supports JUnit argument access and aggregation through
 * {@link org.junit.jupiter.params.aggregator.ArgumentsAccessor ArgumentsAccessor} and
 * {@link org.junit.jupiter.params.aggregator.AggregateWith @AggregateWith}, as well as explicit argument
 * conversion through {@link org.junit.jupiter.params.converter.ConvertWith @ConvertWith}.
 *
 * @since 2.0.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(FixtureArgumentsProvider.class)
public @interface FixtureSource {

    /**
     * Templates whose loaded root fixtures are converted into test arguments.
     *
     * @return fixture templates
     */
    Template[] value();

    /**
     * Determines how the loaded fixtures are converted into test arguments.
     *
     * @return fixture argument generation mode
     * @since 2.1.0
     */
    FixtureSourceMode mode() default FixtureSourceMode.AUTO;
}
