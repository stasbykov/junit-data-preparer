package io.github.stasbykov.datapreparer.test.junit.extension;

import io.github.stasbykov.datapreparer.api.annotation.ClassDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.FixtureInject;
import io.github.stasbykov.datapreparer.api.annotation.FixtureSource;
import io.github.stasbykov.datapreparer.api.annotation.MethodDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.Template;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.AxisAFixture;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.AxisBFixture;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.CycleAFixture;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.OrderFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.AXIS_A_TEMPLATE;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.AXIS_B_TEMPLATE;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.COMPANY_TEMPLATE;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.CYCLE_A_TEMPLATE;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.ORDER_TEMPLATE;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.USER_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class RecursiveAndParameterizedExtensionTest extends BaseTest {

    @BeforeEach
    void resetState() {
        RecursiveFixtureRegistry.clearEvents();
        CartesianFixtureSourceSpec.COMBINATIONS.clear();
    }

    @Test
    void shouldLoadAndDeleteARecursiveRecordGraphInDependencyOrder() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(RecursiveFixtureSpec.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(1).succeeded(1));

        assertEquals(
                java.util.List.of(
                        "load:" + COMPANY_TEMPLATE,
                        "load:" + USER_TEMPLATE,
                        "load:" + ORDER_TEMPLATE,
                        "delete:" + ORDER_TEMPLATE,
                        "delete:" + USER_TEMPLATE,
                        "delete:" + COMPANY_TEMPLATE),
                RecursiveFixtureRegistry.events());
    }

    @Test
    void shouldRejectCircularFixtureReferencesWithFullPath() {
        failureExecutableWithException(
                CircularFixtureSpec.class,
                "someTest",
                ParameterResolutionException.class,
                "Circular fixture reference detected: recursive_cycle_a -> recursive_cycle_b -> recursive_cycle_a");
    }

    @Test
    void shouldCreateCartesianProductForParameterizedTest() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(CartesianFixtureSourceSpec.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(6).succeeded(6));

        assertEquals(Set.of("a0:b0", "a0:b1", "a0:b2", "a1:b0", "a1:b1", "a1:b2"),
                CartesianFixtureSourceSpec.COMBINATIONS);
        assertEquals(List.of(
                        "create:a0", "create:a1", "load:" + AXIS_A_TEMPLATE,
                        "create:b0", "create:b1", "create:b2", "load:" + AXIS_B_TEMPLATE,
                        "delete:" + AXIS_B_TEMPLATE, "delete:" + AXIS_A_TEMPLATE),
                RecursiveFixtureRegistry.events());
    }
}

@ClassDataSetup(value = @Template(name = ORDER_TEMPLATE, count = 2), inject = true)
class RecursiveFixtureSpec {

    @FixtureInject
    FixtureBatchCollection fixtures;

    @Test
    void fixtureContainsLoadedDependenciesAtEveryLevel() {
        assertEquals(2, fixtures.get(ORDER_TEMPLATE, OrderFixture.class).size());
        fixtures.get(ORDER_TEMPLATE, OrderFixture.class).forEach(order -> {
            assertEquals("loaded-order", order.number());
            assertNotNull(order.user());
            assertEquals("loaded-user", order.user().name());
            assertNotNull(order.user().company());
            assertEquals("loaded-company", order.user().company().name());
        });
    }
}

class CircularFixtureSpec {

    @Test
    void someTest(
            @MethodDataSetup(@Template(name = CYCLE_A_TEMPLATE, count = 1))
            FixtureBatchCollection ignored) {
        assertTrue(false, "A circular graph must fail before the test body.");
    }
}

class CartesianFixtureSourceSpec {

    static final Set<String> COMBINATIONS = ConcurrentHashMap.newKeySet();

    @ParameterizedTest
    @FixtureSource({
            @Template(name = AXIS_A_TEMPLATE, count = 2),
            @Template(name = AXIS_B_TEMPLATE, count = 3)
    })
    void receivesEveryCombination(AxisAFixture first, AxisBFixture second) {
        COMBINATIONS.add(first.value() + ":" + second.value());
    }
}
