package io.github.stasbykov.datapreparer.api.core;

import java.util.function.Supplier;

/**
 * The fixture template is described - name, method of adding and removing, as well as the main values of the processed fixture.
 *
 * @param name template name
 * @param loader instance FixtureLoader implementation for fixture
 * @param deleter instance FixtureDeleter implementation for fixture
 * @param data
 * Fixture data for creating test data in tests. For example, users, orders, etc.
 * @param <T> a fixture type for creating test data in tests. For example, users, orders, etc.
 *
 * @see FixtureLoader
 * @see FixtureDeleter
 * @since 1.0.0
 */
public record FixtureTemplate<T extends Fixture>(String name,
                                                 FixtureLoader<T> loader,
                                                 FixtureDeleter<T> deleter,
                                                 Supplier<T> data) {}
