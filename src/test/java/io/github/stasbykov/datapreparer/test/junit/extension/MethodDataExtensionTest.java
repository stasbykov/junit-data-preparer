package io.github.stasbykov.datapreparer.test.junit.extension;

import io.github.stasbykov.datapreparer.api.annotation.ClassDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.FixtureInject;
import io.github.stasbykov.datapreparer.api.annotation.MethodDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.Template;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.TestFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static io.github.stasbykov.datapreparer.test.junit.extension.BaseTest.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class MethodDataExtensionTest extends BaseTest {
    @Test
    void shouldSuccessExecutableExtension() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(MethodDataPositiveTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats ->
                        stats.started(2).succeeded(2));

    }

    @Test
    void shouldFailureExecutableExtensionForZeroCount() {
        failureExecutableWithException(ZeroCountMethodDataSpec.class,
                "someTest",
                ParameterResolutionException.class,
                "The count parameter of the @Template annotation cannot be 0 or negative.");
    }

    @Test
    void shouldFailureExecutableExtensionForNegativeCount() {
        failureExecutableWithException(NegativeCountMethodDataSpec.class,
                "someTest",
                ParameterResolutionException.class,
                "The count parameter of the @Template annotation cannot be 0 or negative.");
    }

    @Test
    void shouldFailureExecutableExtensionForEmptyTemplateName() {
        failureExecutableWithException(EmptyNameTemplateMethodDataSpec.class,
                "someTest",
                ParameterResolutionException.class,
                "The name parameter of the @Template annotation cannot be null or empty.");
    }
}

@ClassDataSetup(
        value = {@Template(name = FIRST_TEMPLATE_NAME, count = FIVE_FIXTURES), @Template(name = SECOND_TEMPLATE_NAME, count = TEN_FIXTURES)},
        inject = true
)
class MethodDataPositiveTest {

    @FixtureInject
    FixtureBatchCollection loadedFixtures;

    @Test
    void shouldSuccessLoadMethodDataAndInjectToMethodField() {
        List<TestFixture> firstClassTestFixtures = loadedFixtures.get(FIRST_TEMPLATE_NAME, TestFixture.class);
        List<TestFixture> secondClassTestFixtures = loadedFixtures.get(SECOND_TEMPLATE_NAME, TestFixture.class);
        List<TestFixture> firstMethodTestFixtures = loadedFixtures.get(FIRST_TEMPLATE_NAME, TestFixture.class);
        List<TestFixture> secondMethodTestFixtures = loadedFixtures.get(SECOND_TEMPLATE_NAME, TestFixture.class);
        assertAll(
                () -> assertEquals(5, firstMethodTestFixtures.size()),
                () -> assertEquals(IntStream.range(0, 5).mapToObj(i -> "Some name for first fixture").toList(), firstMethodTestFixtures.stream().map(TestFixture::name).toList()),
                () -> assertEquals(10, secondMethodTestFixtures.size()),
                () -> assertEquals(IntStream.range(0, 10).mapToObj(i -> "Some name for second fixture").toList(), secondMethodTestFixtures.stream().map(TestFixture::name).toList()),
                () -> assertEquals(FIVE_FIXTURES, firstClassTestFixtures.size()),
                () -> assertEquals(IntStream.range(0, FIVE_FIXTURES).mapToObj(i -> "Some name for first fixture").toList(), firstClassTestFixtures.stream().map(TestFixture::name).toList()),
                () -> assertEquals(TEN_FIXTURES, secondClassTestFixtures.size()),
                () -> assertEquals(IntStream.range(0, TEN_FIXTURES).mapToObj(i -> "Some name for second fixture").toList(), secondClassTestFixtures.stream().map(TestFixture::name).toList()));
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    public void shouldSuccessPerformanceLoadMethodDataAndInjectToMethodArgument(@MethodDataSetup({@Template(name = FIRST_TEMPLATE_NAME, count = TEN_THOUSAND_FIXTURES), @Template(name = SECOND_TEMPLATE_NAME, count = FIVE_FIXTURES)}) FixtureBatchCollection collection) {
        List<TestFixture> firstTestFixtures = collection.get(FIRST_TEMPLATE_NAME, TestFixture.class);
        List<TestFixture> secondTestFixtures = collection.get(SECOND_TEMPLATE_NAME, TestFixture.class);
        assertAll(
                () -> assertEquals(TEN_THOUSAND_FIXTURES, firstTestFixtures.size()),
                () -> assertEquals(IntStream.range(0, TEN_THOUSAND_FIXTURES).mapToObj(i -> "Some name for first fixture").toList(), firstTestFixtures.stream().map(TestFixture::name).toList()),
                () -> assertEquals(FIVE_FIXTURES, secondTestFixtures.size()),
                () -> assertEquals(IntStream.range(0, FIVE_FIXTURES).mapToObj(i -> "Some name for second fixture").toList(), secondTestFixtures.stream().map(TestFixture::name).toList())
        );
    }
}

class ZeroCountMethodDataSpec {
    @Test
    public void someTest(@MethodDataSetup({@Template(name = FIRST_TEMPLATE_NAME, count = FIVE_FIXTURES), @Template(name = SECOND_TEMPLATE_NAME, count = ZERO_FIXTURES)}) FixtureBatchCollection collection) {}
}

class NegativeCountMethodDataSpec {
    @Test
    public void someTest(@MethodDataSetup({@Template(name = FIRST_TEMPLATE_NAME, count = FIVE_FIXTURES), @Template(name = SECOND_TEMPLATE_NAME, count = NEGATIVE_COUNT_FIXTURE)}) FixtureBatchCollection collection) {}
}

class EmptyNameTemplateMethodDataSpec {
    @Test
    public void someTest(@MethodDataSetup({@Template(name = FIRST_TEMPLATE_NAME, count = FIVE_FIXTURES), @Template(name = EMPTY_TEMPLATE_NAME, count = TEN_FIXTURES)}) FixtureBatchCollection collection) {}
}

