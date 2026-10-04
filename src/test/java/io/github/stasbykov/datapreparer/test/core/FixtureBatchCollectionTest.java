package io.github.stasbykov.datapreparer.test.core;

import io.github.stasbykov.datapreparer.api.core.FixtureBatch;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.api.core.FixtureTemplate;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.TestFixture;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixtureBatchCollectionTest {

    @Test
    void shouldDeleteEveryBatchInReverseOrderWhenDeletersFail() {
        List<String> deletedTemplates = new ArrayList<>();
        FixtureBatchCollection collection = new FixtureBatchCollection(List.of(
                batch("first", deletedTemplates, false),
                batch("second", deletedTemplates, true),
                batch("third", deletedTemplates, true)));

        IllegalStateException exception = assertThrows(IllegalStateException.class, collection::close);

        assertEquals("Failed to delete third", exception.getMessage());
        assertEquals(1, exception.getSuppressed().length);
        assertEquals("Failed to delete second", exception.getSuppressed()[0].getMessage());
        assertEquals(List.of("third", "second", "first"), deletedTemplates);
    }

    private FixtureBatch<TestFixture> batch(
            String templateName,
            List<String> deletedTemplates,
            boolean failOnDeletion) {
        TestFixture fixture = new TestFixture(templateName, templateName);
        FixtureTemplate<TestFixture> template = new FixtureTemplate<>(
                templateName,
                List::copyOf,
                fixtures -> {
                    deletedTemplates.add(templateName);
                    if (failOnDeletion) {
                        throw new IllegalStateException("Failed to delete " + templateName);
                    }
                },
                () -> fixture);
        return new FixtureBatch<>(template, List.of(fixture));
    }
}
