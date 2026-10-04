package io.github.stasbykov.datapreparer.api.core;

import java.util.List;

/**
 * Helper class for storing a package of fixtures.
 *
 * @param <T> type of fixture
 * @param template template that produced the fixtures
 * @param fixtures fixtures produced from the template
 *
 * @see FixtureTemplate
 * @since 1.0.0
 */
public record FixtureBatch<T extends Fixture>(FixtureTemplate<T> template, List<T> fixtures) {}
