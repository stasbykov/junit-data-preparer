package io.github.stasbykov.datapreparer.test.junit.extension.fixture;

import io.github.stasbykov.datapreparer.api.core.Fixture;

public record TestFixture(String name, String value) implements Fixture {
}
