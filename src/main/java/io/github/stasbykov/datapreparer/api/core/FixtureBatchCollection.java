package io.github.stasbykov.datapreparer.api.core;

import io.github.stasbykov.datapreparer.internal.junit.TestDataPreparer;
import org.jetbrains.annotations.NotNull;

import java.util.List;


/**
 * A record representing a collection of fixture batches used for test data preparation.
 * Provides methods to retrieve specific fixtures by template name and type,
 * and implements {@link AutoCloseable} to allow for cleanup after use.
 *
 * @see FixtureBatch
 * @since 1.0.0
 */
public record FixtureBatchCollection(List<FixtureBatch<? extends Fixture>> batches) implements AutoCloseable {

    /**
     * Retrieves a list of fixtures that match the given template name and are instances of the specified type.
     *
     * @param templateName the name of the template to filter by; must not be null
     * @param type the class type of the desired fixtures; must not be null
     * @param <T> the type of the fixtures to retrieve, extending {@link Fixture}
     * @return a list of fixtures matching the template name and type
     * @throws NullPointerException if either {@code templateName} or {@code type} is null
     */
    public <T extends Fixture> List<T> get(@NotNull String templateName, @NotNull Class<T> type) {
        return batches.stream()
                .filter(batch-> templateName.equals(batch.template().name()))
                .map(FixtureBatch::fixtures)
                .flatMap(List::stream)
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    /**
     * Cleanup after use.
     *
     * @throws Exception throw an exception if an error occurs during deletion
     */
    @Override
    public void close() throws Exception {
        TestDataPreparer.processTemplatesForDeletion(batches);
    }
}