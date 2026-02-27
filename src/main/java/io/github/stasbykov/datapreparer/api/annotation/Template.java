package io.github.stasbykov.datapreparer.api.annotation;

import io.github.stasbykov.datapreparer.api.junit.MethodDataPrepareExtension;
import io.github.stasbykov.datapreparer.api.junit.ClassDataPrepareExtension;

import java.lang.annotation.*;

/**
 * Describes the required template and sets the number of copies.
 * <p>
 * This annotation is used to provide metadata about a template, such as its logical name
 * and the number of instances that should be generated.
 * </p>
 *
 * @see ClassDataSetup
 * @see MethodDataSetup
 * @see ClassDataPrepareExtension
 * @see MethodDataPrepareExtension
 * @since 1.0
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Template {
    String name();
    int count();
}
