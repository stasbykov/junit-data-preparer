package io.github.stasbykov.datapreparer.test.junit.extension;

import io.github.stasbykov.datapreparer.api.annotation.ClassDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.FixtureInject;
import io.github.stasbykov.datapreparer.api.annotation.Template;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.TestFixture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static io.github.stasbykov.datapreparer.test.junit.extension.BaseTest.*;
import static io.github.stasbykov.datapreparer.test.junit.extension.ClassDataExtensionTest.TEN_FIXTURES;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.AXIS_A_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class ClassDataExtensionTest extends BaseTest {

    @Test
    void shouldSuccessExecutableExtension() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(ClassDataPositiveTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats ->
                        stats.started(2).succeeded(2));

    }

    @Test
    void shouldFailureExecutableExtensionForZeroCount() {
        failureBeforeTestExecutionWithException(ZeroCountClassDataSpec.class,
                IllegalArgumentException.class,
                "The count parameter of the @Template annotation cannot be 0 or negative.");
    }

    @Test
    void shouldFailureExecutableExtensionForNegativeCount() {
        failureBeforeTestExecutionWithException(NegativeCountClassDataSpec.class,
                IllegalArgumentException.class,
                "The count parameter of the @Template annotation cannot be 0 or negative.");
    }

    @Test
    void shouldFailureExecutableExtensionForEmptyTemplateName() {
        failureBeforeTestExecutionWithException(EmptyNameTemplateClassDataSpec.class,
                IllegalArgumentException.class,
                "The name parameter of the @Template annotation cannot be null or empty.");
    }

    @Test
    void shouldFailureExecutableExtensionForIncorrectInjectFieldType() {
        failureExecutableWithException(IncorrectInjectFieldTypeClassDataSpec.class,
                "someTest",
                IllegalArgumentException.class,
                "Fixture saving failed because the field annotated with @FixtureInject must be of type FixtureBatchCollection.");
    }

    @Test
    void shouldRejectUnknownTemplateBeforeTestExecution() {
        RecursiveFixtureRegistry.clearEvents();

        failureBeforeTestExecutionWithException(UnknownTemplateClassDataSpec.class,
                IllegalArgumentException.class,
                "Fixture template was not found: unknown_template");

        assertEquals(
                List.of("create:a0", "load:" + AXIS_A_TEMPLATE, "delete:" + AXIS_A_TEMPLATE),
                RecursiveFixtureRegistry.events());
    }

    @Test
    void shouldPrepareFixturesBeforeStaticBeforeAll() {
        RecursiveFixtureRegistry.clearEvents();
        BeforeAllClassDataSpec.fixturesWereLoaded = false;

        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(BeforeAllClassDataSpec.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1));

        assertTrue(BeforeAllClassDataSpec.fixturesWereLoaded);
        assertEquals(
                List.of("create:a0", "load:" + AXIS_A_TEMPLATE, "delete:" + AXIS_A_TEMPLATE),
                RecursiveFixtureRegistry.events());
    }

    @Test
    void shouldInjectFixturesIntoInheritedField() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(InheritedInjectionClassDataSpec.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1));
    }
}

@ClassDataSetup(
        value = {@Template(name = FIRST_TEMPLATE_NAME, count = TEN_THOUSAND_FIXTURES), @Template(name = SECOND_TEMPLATE_NAME, count = TEN_FIXTURES)},
        inject = true
)
class ClassDataPositiveTest {

    @FixtureInject
    FixtureBatchCollection loadedFixtures;

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void shouldSuccessLoadClassDataAndInjectToClassField() {
        List<TestFixture> firstTestFixtures = loadedFixtures.get(FIRST_TEMPLATE_NAME, TestFixture.class);
        List<TestFixture> secondTestFixtures = loadedFixtures.get(SECOND_TEMPLATE_NAME, TestFixture.class);
        assertAll(
                () -> assertEquals(TEN_THOUSAND_FIXTURES, firstTestFixtures.size()),
                () -> assertEquals(IntStream.range(0, TEN_THOUSAND_FIXTURES).mapToObj(i -> "Some name for first fixture").toList(), firstTestFixtures.stream().map(TestFixture::name).toList()),
                () -> assertEquals(TEN_FIXTURES, secondTestFixtures.size()),
                () -> assertEquals(IntStream.range(0, TEN_FIXTURES).mapToObj(i -> "Some name for second fixture").toList(), secondTestFixtures.stream().map(TestFixture::name).toList()));
    }

    @Test
    void anotherTest() {
    }
}

@ClassDataSetup(
        value = {@Template(name = FIRST_TEMPLATE_NAME, count = TEN_FIXTURES), @Template(name = SECOND_TEMPLATE_NAME, count = ZERO_FIXTURES)}
)
class ZeroCountClassDataSpec {
    @Test
    void someTest() {
    }
}

@ClassDataSetup(
        value = {@Template(name = FIRST_TEMPLATE_NAME, count = TEN_FIXTURES), @Template(name = SECOND_TEMPLATE_NAME, count = NEGATIVE_COUNT_FIXTURE)}
)
class NegativeCountClassDataSpec {
    @Test
    void someTest() {
    }
}

@ClassDataSetup(
        value = {@Template(name = FIRST_TEMPLATE_NAME, count = TEN_FIXTURES), @Template(name = EMPTY_TEMPLATE_NAME, count = TEN_FIXTURES)}
)
class EmptyNameTemplateClassDataSpec {
    @Test
    void someTest() {
    }
}

@ClassDataSetup(
        value = {@Template(name = FIRST_TEMPLATE_NAME, count = TEN_FIXTURES), @Template(name = SECOND_TEMPLATE_NAME, count = TEN_FIXTURES)},
        inject = true
)
class IncorrectInjectFieldTypeClassDataSpec {
    @FixtureInject
    String loadedFixtures;
    @Test
    void someTest() {
    }
}

@ClassDataSetup({
        @Template(name = AXIS_A_TEMPLATE, count = 1),
        @Template(name = "unknown_template", count = 1)})
class UnknownTemplateClassDataSpec {
    @Test
    void someTest() {
    }
}

@ClassDataSetup(@Template(name = AXIS_A_TEMPLATE, count = 1))
class BeforeAllClassDataSpec {
    static boolean fixturesWereLoaded;

    @BeforeAll
    static void beforeAll() {
        fixturesWereLoaded = RecursiveFixtureRegistry.events()
                .contains("load:" + AXIS_A_TEMPLATE);
    }

    @Test
    void someTest() {
    }
}

class ClassDataInjectionBase {
    @FixtureInject
    FixtureBatchCollection loadedFixtures;
}

@ClassDataSetup(value = @Template(name = FIRST_TEMPLATE_NAME, count = 1), inject = true)
class InheritedInjectionClassDataSpec extends ClassDataInjectionBase {
    @Test
    void someTest() {
        assertNotNull(loadedFixtures);
        assertEquals(1, loadedFixtures.get(FIRST_TEMPLATE_NAME, TestFixture.class).size());
    }
}




