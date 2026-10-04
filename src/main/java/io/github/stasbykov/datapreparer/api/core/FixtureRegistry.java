package io.github.stasbykov.datapreparer.api.core;

import java.util.List;

/**
 * Fixture register
 *
 * @param <T> type of fixture
 *
 * @since 1.0.0
 */
public interface FixtureRegistry<T extends Fixture> {
    /**
     * Returns the fixture templates exposed by this registry.
     *
     * @return registered fixture templates
     */
    List<FixtureTemplate<T>> getTemplates();
}
