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
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void shouldRejectUnknownTemplate() {
        failureExecutableWithException(UnknownTemplateMethodDataSpec.class,
                "someTest",
                ParameterResolutionException.class,
                "Fixture template was not found: unknown_template");
    }

    @Test
    void shouldPrepareDifferentFixturesForMultipleParameters() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(MultipleMethodDataParametersSpec.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1));
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
    void shouldKeepClassAndMethodDataSeparate(
            @MethodDataSetup({
                    @Template(name = FIRST_TEMPLATE_NAME, count = 2),
                    @Template(name = SECOND_TEMPLATE_NAME, count = 3)}) FixtureBatchCollection methodFixtures) {
        List<TestFixture> firstClassTestFixtures = loadedFixtures.get(FIRST_TEMPLATE_NAME, TestFixture.class);
        List<TestFixture> secondClassTestFixtures = loadedFixtures.get(SECOND_TEMPLATE_NAME, TestFixture.class);
        List<TestFixture> firstMethodTestFixtures = methodFixtures.get(FIRST_TEMPLATE_NAME, TestFixture.class);
        List<TestFixture> secondMethodTestFixtures = methodFixtures.get(SECOND_TEMPLATE_NAME, TestFixture.class);
        assertAll(
                () -> assertEquals(2, firstMethodTestFixtures.size()),
                () -> assertEquals(IntStream.range(0, 2).mapToObj(i -> "Some name for first fixture").toList(), firstMethodTestFixtures.stream().map(TestFixture::name).toList()),
                () -> assertEquals(3, secondMethodTestFixtures.size()),
                () -> assertEquals(IntStream.range(0, 3).mapToObj(i -> "Some name for second fixture").toList(), secondMethodTestFixtures.stream().map(TestFixture::name).toList()),
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

class UnknownTemplateMethodDataSpec {
    @Test
    void someTest(
            @MethodDataSetup(@Template(name = "unknown_template", count = 1))
            FixtureBatchCollection collection) {
    }
}

class MultipleMethodDataParametersSpec {
    @Test
    void someTest(
            @MethodDataSetup(@Template(name = FIRST_TEMPLATE_NAME, count = 1))
            FixtureBatchCollection first,
            @MethodDataSetup(@Template(name = SECOND_TEMPLATE_NAME, count = 2))
            FixtureBatchCollection second) {
        assertAll(
                () -> assertEquals(1, first.get(FIRST_TEMPLATE_NAME, TestFixture.class).size()),
                () -> assertTrue(first.get(SECOND_TEMPLATE_NAME, TestFixture.class).isEmpty()),
                () -> assertTrue(second.get(FIRST_TEMPLATE_NAME, TestFixture.class).isEmpty()),
                () -> assertEquals(2, second.get(SECOND_TEMPLATE_NAME, TestFixture.class).size()));
    }
}

