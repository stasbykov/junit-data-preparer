package io.github.stasbykov.datapreparer.api.junit;

import io.github.stasbykov.datapreparer.api.annotation.FixtureSource;
import io.github.stasbykov.datapreparer.api.annotation.Template;
import io.github.stasbykov.datapreparer.api.core.Fixture;
import io.github.stasbykov.datapreparer.api.core.FixtureBatch;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.internal.core.FixtureHandler;
import io.github.stasbykov.datapreparer.internal.junit.FixturePreparation;
import io.github.stasbykov.datapreparer.internal.junit.TestDataPreparer;
import io.github.stasbykov.datapreparer.internal.util.scanner.ClassgraphScanner;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.Arrays;
import java.util.List;
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

    /**
     * Creates a provider. JUnit configures it from {@link FixtureSource} before use.
     */
    public FixtureArgumentsProvider() {
    }

    @Override
    public void accept(FixtureSource source) {
        templates = source.value().clone();
    }

    @Override
    public Stream<? extends Arguments> provideArguments(
            ParameterDeclarations parameters,
            ExtensionContext context) {
        if (templates == null || templates.length == 0) {
            throw new IllegalArgumentException("@FixtureSource must declare at least one @Template.");
        }

        TestDataPreparer preparer = new TestDataPreparer(new FixtureHandler(new ClassgraphScanner()));
        FixturePreparation preparation = preparer.prepareFixtures(templates);
        if (preparation.roots().size() != templates.length) {
            TestDataPreparer.processTemplatesForDeletion(preparation.batches());
            throw new IllegalArgumentException("Every @FixtureSource template must resolve to a registered fixture template.");
        }

        context.getStore(NAMESPACE).put(
                context.getUniqueId(),
                new FixtureBatchCollection(preparation.batches()));

        Stream<Object[]> combinations = Stream.<Object[]>of(new Object[0]);
        for (FixtureBatch<? extends Fixture> root : preparation.roots()) {
            List<? extends Fixture> axis = root.fixtures();
            combinations = combinations.flatMap(prefix -> axis.stream()
                    .map(value -> append(prefix, value)));
        }
        return combinations.map(values -> Arguments.of(values));
    }

    private Object[] append(Object[] prefix, Object value) {
        Object[] result = Arrays.copyOf(prefix, prefix.length + 1);
        result[prefix.length] = value;
        return result;
    }
}
