package io.github.stasbykov.datapreparer.api.core;

import java.util.List;

/**
 * Fixture Loader Interface. Used to load fixtures.
 *
 * @param <T> is the type of fixture to be created. Must be implementation of the {@link Fixture} class.
 *
 * @since 1.0.0
 */
public interface FixtureLoader<T extends Fixture> {
    /**
     * Loads the supplied fixture definitions and returns their prepared representations.
     *
     * @param fixture fixture definitions to load
     * @return prepared fixtures
     */
    List<T> load(List<T> fixture);
}
