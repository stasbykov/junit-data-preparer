package io.github.stasbykov.datapreparer.test.junit.extension.fixture;

import io.github.stasbykov.datapreparer.api.core.FixtureDeleter;
import io.github.stasbykov.datapreparer.api.core.FixtureLoader;
import io.github.stasbykov.datapreparer.api.core.FixtureRegistry;
import io.github.stasbykov.datapreparer.api.core.FixtureTemplate;

import java.util.List;
import java.util.UUID;

public class TestFixtureRegistry implements FixtureRegistry<TestFixture> {
    @Override
    public List<FixtureTemplate<TestFixture>> getTemplates() {
        return List.of(
               new FixtureTemplate<TestFixture>(
                       "test_template_1",
                       new FixtureLoader<TestFixture>() {
                           @Override
                           public List<TestFixture> load(List<TestFixture> fixture) {
                               System.out.println("Starts loading first fixtures");
                               return fixture;
                           }
                       },
                       new FixtureDeleter<TestFixture>() {
                           @Override
                           public void delete(List<TestFixture> fixture) {
                               System.out.println("Starts deleting first fixtures");
                           }
                       },
                       () -> new TestFixture("Some name for first fixture", UUID.randomUUID().toString())),
                new FixtureTemplate<TestFixture>(
                        "test_template_2",
                        new FixtureLoader<TestFixture>() {
                            @Override
                            public List<TestFixture> load(List<TestFixture> fixture) {
                                System.out.println("Starts loading second fixtures");
                                return fixture;
                            }
                        },
                        new FixtureDeleter<TestFixture>() {
                            @Override
                            public void delete(List<TestFixture> fixture) {
                                System.out.println("Starts deleting second fixtures");
                            }
                        },
                        () -> new TestFixture("Some name for second fixture", UUID.randomUUID().toString()))
        );
    }
}
