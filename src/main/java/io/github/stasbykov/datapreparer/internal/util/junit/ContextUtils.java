package io.github.stasbykov.datapreparer.internal.util.junit;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Helper class for working with the Junit5 context {@code ExtensionContext}
 *
 * @since 1.0.0
 */
public final class ContextUtils {

    private ContextUtils() {}

    /**
     * Gets an annotation of the specified type from the JUnit test context.
     *
     * @param context JUnit test context
     * @param annotationType type of annotation to find
     * @param <T> annotation type
     * @return optional value of the annotation, if present; otherwise, an empty optional value
     */
    public static <T extends Annotation> Optional<T> getAnnotation(
            @NotNull ExtensionContext context,
            @NotNull Class<T> annotationType) {

        return context.getElement()
                .flatMap(element -> AnnotationSupport.findAnnotation(element, annotationType));
    }

    /**
     * Gets a required annotation of the specified type from the JUnit test context.
     * If the annotation is missing, throws an {@link IllegalStateException}.
     *
     * @param context JUnit test context
     * @param annotationType the type of annotation to find
     * @param <T> the annotation type
     * @return the annotation of the specified type
     * @throws IllegalStateException if the annotation is not found
     */
    public static <T extends Annotation> T getRequiredAnnotation(
            @NotNull ExtensionContext context,
            @NotNull Class<T> annotationType) {
        String message = String.format("The required mandatory annotation %s was not found.", annotationType.getName());
        return getAnnotation(
                context,
                annotationType).orElseThrow(() -> new IllegalStateException(message));
    }

}
