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
    List<FixtureTemplate<T>> getTemplates();
}
