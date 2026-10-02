package io.github.stasbykov.datapreparer.internal.junit;

import io.github.stasbykov.datapreparer.api.core.Fixture;
import io.github.stasbykov.datapreparer.api.core.FixtureBatch;

import java.util.List;

/**
 * Result of preparing requested fixture graphs.
 *
 * @param batches all loaded batches in dependency-first order
 * @param roots batches explicitly requested by the caller
 */
public record FixturePreparation(
        List<FixtureBatch<? extends Fixture>> batches,
        List<FixtureBatch<? extends Fixture>> roots) {

    public FixturePreparation {
        batches = List.copyOf(batches);
        roots = List.copyOf(roots);
    }
}
