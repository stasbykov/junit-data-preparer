package io.github.stasbykov.datapreparer.test.junit.extension;

import org.assertj.core.api.Condition;
import io.github.stasbykov.datapreparer.api.core.FixtureRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest {
    /**
     * Значение параметра для хранения названия пакета с {@link FixtureRegistry} по умолчанию
     */
    private final static String PACKAGE_NAME_PROPERTIES = "fixture.package.registry";

    /**
     * Кастомный пакет для хранения {@link FixtureRegistry}
     */
    private final static String CUSTOM_PACKAGE_REGISTRY = "io.github.stasbykov.datapreparer.test";

    /**
     * Константы для тестирования количества фикстур
     */
    protected final static int TEN_THOUSAND_FIXTURES = 10000;
    protected final static int FIVE_FIXTURES = 5;
    protected final static int TEN_FIXTURES = 10;
    protected final static int ZERO_FIXTURES = 0;
    protected final static int NEGATIVE_COUNT_FIXTURE = -1;

    /**
     *
     */
    protected final static String FIRST_TEMPLATE_NAME = "test_template_1";
    protected final static String SECOND_TEMPLATE_NAME = "test_template_2";
    protected final static String EMPTY_TEMPLATE_NAME = "";

    @BeforeAll
    void setUp() {
        System.setProperty(PACKAGE_NAME_PROPERTIES, CUSTOM_PACKAGE_REGISTRY);
    }

    @AfterAll
    void tearDown() {
        System.clearProperty(PACKAGE_NAME_PROPERTIES);
    }

    void failureExecutableWithException(Class<?> testClass, String methodName, Class<? extends Exception> exceptionClass, String errorMessage) {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(testClass))
                .execute()
                .testEvents()
                .assertStatistics(stats ->
                        stats.started(1).succeeded(0))
                .assertThatEvents()
                .haveExactly(1,
                        event(
                                test(methodName),
                                finishedWithFailure(
                                        new Condition<>(
                                                throwable -> throwable.getMessage() != null &&
                                                        throwable.getMessage().contains(errorMessage) &&
                                                        exceptionClass.isInstance(throwable),
                                                "The error message contains: " + errorMessage
                                        )
                                )
                        ));

    }
}
