package io.github.stasbykov.datapreparer.api.annotation;

import io.github.stasbykov.datapreparer.api.core.Fixture;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a {@link Fixture} field is populated from another named fixture template.
 * Referenced fixtures are loaded before the fixture that contains the field.
 *
 * @since 2.0.0
 */
@Documented
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface FixtureReference {

    /**
     * Name of the referenced fixture template.
     *
     * @return template name
     */
    String value();
}
