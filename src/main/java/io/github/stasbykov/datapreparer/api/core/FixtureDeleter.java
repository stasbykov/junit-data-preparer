package io.github.stasbykov.datapreparer.api.core;

import java.util.List;

/**
 * Fixture Clearing Interface. Used to delete fixtures.
 *
 * @param <T> is the type of fixture to be removed. Must be implementation of the {@link Fixture} class.
 *
 * @since 1.0.0
 */
public interface FixtureDeleter<T extends Fixture> {
    /**
     * Deletes the supplied fixtures.
     *
     * @param fixture fixtures to delete
     */
    void delete(List<T> fixture);
}
