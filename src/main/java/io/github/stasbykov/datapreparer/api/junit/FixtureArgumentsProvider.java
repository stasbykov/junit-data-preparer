package io.github.stasbykov.datapreparer.api.junit;

import io.github.stasbykov.datapreparer.api.annotation.FixtureSource;
import io.github.stasbykov.datapreparer.api.annotation.FixtureSourceMode;
import io.github.stasbykov.datapreparer.api.annotation.Template;
import io.github.stasbykov.datapreparer.api.core.Fixture;
import io.github.stasbykov.datapreparer.api.core.FixtureBatch;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.internal.core.FixtureHandler;
import io.github.stasbykov.datapreparer.internal.junit.FixturePreparation;
import io.github.stasbykov.datapreparer.internal.junit.TestDataPreparer;
import io.github.stasbykov.datapreparer.internal.util.scanner.ClassgraphScanner;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.junit.jupiter.params.support.ParameterDeclaration;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Creates parameterized-test arguments from fixture templates.
 *
 * @since 2.0.0
 */
public final class FixtureArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<FixtureSource> {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(FixtureArgumentsProvider.class);

    private Template[] templates;
    private FixtureSourceMode mode;

    /**
     * Creates a provider. JUnit configures it from {@link FixtureSource} before use.
     */
    public FixtureArgumentsProvider() {
    }

    @Override
    public void accept(FixtureSource source) {
        templates = source.value().clone();
        mode = source.mode();
    }

    @Override
    public @NonNull Stream<? extends Arguments> provideArguments(
            @NonNull ParameterDeclarations parameters,
            @NonNull ExtensionContext context) {
        if (templates == null || templates.length == 0) {
            throw new IllegalArgumentException("@FixtureSource must declare at least one @Template.");
        }
        boolean aggregatorPresent = hasAggregator(parameters);
        FixtureSourceMode resolvedMode = resolveMode(parameters, aggregatorPresent);
        validateParameterCount(resolvedMode, parameters, aggregatorPresent);

        TestDataPreparer preparer = new TestDataPreparer(new FixtureHandler(new ClassgraphScanner()));
        FixturePreparation preparation = preparer.prepareFixtures(templates);
        if (preparation.roots().size() != templates.length) {
            TestDataPreparer.processTemplatesForDeletion(preparation.batches());
            throw new IllegalArgumentException("Every @FixtureSource template must resolve to a registered fixture template.");
        }
        try {
            validateArgumentTypes(resolvedMode, parameters, preparation.roots());
        } catch (RuntimeException | Error exception) {
            cleanupAfterValidationFailure(preparation, exception);
            throw exception;
        }

        context.getStore(NAMESPACE).put(
                context.getUniqueId(),
                new FixtureBatchCollection(preparation.batches()));

        return switch (resolvedMode) {
            case EACH -> eachFixture(preparation.roots());
            case CARTESIAN -> cartesianProduct(preparation.roots());
            case AUTO -> throw new IllegalStateException("AUTO mode must be resolved before creating arguments.");
        };
    }

    private FixtureSourceMode resolveMode(
            ParameterDeclarations parameters,
            boolean aggregatorPresent) {
        if (mode != FixtureSourceMode.AUTO) {
            return mode;
        }
        return parameters.getAll().size() == 1 && !aggregatorPresent
                ? FixtureSourceMode.EACH
                : FixtureSourceMode.CARTESIAN;
    }

    private void validateParameterCount(
            FixtureSourceMode resolvedMode,
            ParameterDeclarations parameters,
            boolean aggregatorPresent) {
        int actualCount = parameters.getAll().size();
        int expectedCount = resolvedMode == FixtureSourceMode.EACH ? 1 : templates.length;
        if (!aggregatorPresent && actualCount != expectedCount) {
            throw new IllegalArgumentException(
                    "@FixtureSource mode %s requires %d indexed test-method parameter(s), but found %d."
                            .formatted(resolvedMode, expectedCount, actualCount));
        }
        if (aggregatorPresent && actualCount > expectedCount) {
            throw new IllegalArgumentException(
                    "@FixtureSource mode %s supports at most %d indexed test-method parameter(s) "
                            + "when an argument aggregator is declared, but found %d."
                            .formatted(resolvedMode, expectedCount, actualCount));
        }
    }

    private void validateArgumentTypes(
            FixtureSourceMode resolvedMode,
            ParameterDeclarations parameters,
            List<FixtureBatch<? extends Fixture>> roots) {
        if (resolvedMode == FixtureSourceMode.EACH) {
            parameters.getAll().stream().findFirst()
                    .ifPresent(parameter -> roots.forEach(root -> validateBatchType(root, parameter)));
            return;
        }
        for (int index = 0; index < parameters.getAll().size(); index++) {
            validateBatchType(roots.get(index), parameters.getAll().get(index));
        }
    }

    private void validateBatchType(
            FixtureBatch<? extends Fixture> batch,
            ParameterDeclaration parameter) {
        if (hasAnnotation(parameter.getAnnotatedElement(), ConvertWith.class)) {
            return;
        }
        batch.fixtures().stream()
                .filter(fixture -> !parameter.getParameterType().isInstance(fixture))
                .findFirst()
                .ifPresent(fixture -> {
                    throw new IllegalArgumentException(
                            "@FixtureSource template '%s' produced %s, which cannot be assigned to parameter %d of type %s."
                                    .formatted(
                                            batch.template().name(),
                                            fixture.getClass().getName(),
                                            parameter.getParameterIndex(),
                                            parameter.getParameterType().getName()));
                });
    }

    private boolean hasAggregator(ParameterDeclarations parameters) {
        if (!(parameters.getSourceElement() instanceof Executable executable)) {
            return false;
        }
        return Arrays.stream(executable.getParameters())
                .anyMatch(parameter -> ArgumentsAccessor.class.isAssignableFrom(parameter.getType())
                        || hasAnnotation(parameter, AggregateWith.class));
    }

    private boolean hasAnnotation(
            AnnotatedElement element,
            Class<? extends Annotation> annotationType) {
        return element != null && hasAnnotation(element, annotationType, new HashSet<>());
    }

    private boolean hasAnnotation(
            AnnotatedElement element,
            Class<? extends Annotation> annotationType,
            Set<Class<? extends Annotation>> visited) {
        if (element.isAnnotationPresent(annotationType)) {
            return true;
        }
        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> candidate = annotation.annotationType();
            if (candidate.getPackageName().equals("java.lang.annotation") || !visited.add(candidate)) {
                continue;
            }
            if (hasAnnotation(candidate, annotationType, visited)) {
                return true;
            }
        }
        return false;
    }

    private void cleanupAfterValidationFailure(FixturePreparation preparation, Throwable originalException) {
        try {
            TestDataPreparer.processTemplatesForDeletion(preparation.batches());
        } catch (RuntimeException | Error cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private Stream<? extends Arguments> eachFixture(List<FixtureBatch<? extends Fixture>> roots) {
        return roots.stream()
                .map(FixtureBatch::fixtures)
                .flatMap(List::stream)
                .map(Arguments::of);
    }

    private Stream<? extends Arguments> cartesianProduct(List<FixtureBatch<? extends Fixture>> roots) {
        Stream<Object[]> combinations = Stream.<Object[]>of(new Object[0]);
        for (FixtureBatch<? extends Fixture> root : roots) {
            List<? extends Fixture> axis = root.fixtures();
            combinations = combinations.flatMap(prefix -> axis.stream()
                    .map(value -> append(prefix, value)));
        }
        return combinations.map(Arguments::of);
    }

    private Object[] append(Object[] prefix, Object value) {
        Object[] result = Arrays.copyOf(prefix, prefix.length + 1);
        result[prefix.length] = value;
        return result;
    }
}
