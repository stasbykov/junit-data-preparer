package io.github.stasbykov.datapreparer.test.junit.extension;

import io.github.stasbykov.datapreparer.api.annotation.ClassDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.FixtureInject;
import io.github.stasbykov.datapreparer.api.annotation.FixtureSource;
import io.github.stasbykov.datapreparer.api.annotation.FixtureSourceMode;
import io.github.stasbykov.datapreparer.api.annotation.MethodDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.Template;
import io.github.stasbykov.datapreparer.api.core.Fixture;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.api.junit.FixtureArgumentsProvider;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.AxisAFixture;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.AxisBFixture;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.CycleAFixture;
import io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.OrderFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.support.ParameterDeclaration;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.stasbykov.datapreparer.api.annotation.FixtureSourceMode.EACH;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.AXIS_A_TEMPLATE;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.AXIS_B_TEMPLATE;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.COMPANY_TEMPLATE;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.CYCLE_A_TEMPLATE;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.ORDER_TEMPLATE;
import static io.github.stasbykov.datapreparer.test.junit.extension.fixture.RecursiveFixtureRegistry.USER_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecursiveAndParameterizedExtensionTest extends BaseTest {

    @BeforeEach
    void resetState() {
        RecursiveFixtureRegistry.clearEvents();
        AutoEachFixtureSourceSpec.VALUES.clear();
        EachFixtureSourceSpec.VALUES.clear();
        CartesianFixtureSourceSpec.COMBINATIONS.clear();
        ArgumentsAccessorFixtureSourceSpec.COMBINATIONS.clear();
        AggregatedFixtureSourceSpec.COMBINATIONS.clear();
        ConvertedFixtureSourceSpec.VALUES.clear();
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

    @Test
    void shouldCreateOneInvocationForEachRepeatedTemplate() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(AutoEachFixtureSourceSpec.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(2).succeeded(2));

        assertEquals(Set.of("a0", "a1"), AutoEachFixtureSourceSpec.VALUES);
        assertEquals(List.of(
                        "create:a0", "load:" + AXIS_A_TEMPLATE,
                        "create:a1", "load:" + AXIS_A_TEMPLATE,
                        "delete:" + AXIS_A_TEMPLATE, "delete:" + AXIS_A_TEMPLATE),
                RecursiveFixtureRegistry.events());
    }

    @Test
    void shouldCreateOneInvocationForEveryCompatibleFixtureInAutoMode() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(EachFixtureSourceSpec.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(5).succeeded(5));

        assertEquals(Set.of("a0", "a1", "b0", "b1", "b2"), EachFixtureSourceSpec.VALUES);
        assertEquals(List.of(
                        "create:a0", "create:a1", "load:" + AXIS_A_TEMPLATE,
                        "create:b0", "create:b1", "create:b2", "load:" + AXIS_B_TEMPLATE,
                        "delete:" + AXIS_B_TEMPLATE, "delete:" + AXIS_A_TEMPLATE),
                RecursiveFixtureRegistry.events());
    }

    @Test
    void shouldCreateCartesianArgumentsForArgumentsAccessor() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(ArgumentsAccessorFixtureSourceSpec.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(1).succeeded(1));

        assertEquals(Set.of("a0:b0"), ArgumentsAccessorFixtureSourceSpec.COMBINATIONS);
        assertEquals(List.of(
                        "create:a0", "load:" + AXIS_A_TEMPLATE,
                        "create:b0", "load:" + AXIS_B_TEMPLATE,
                        "delete:" + AXIS_B_TEMPLATE, "delete:" + AXIS_A_TEMPLATE),
                RecursiveFixtureRegistry.events());
    }

    @Test
    void shouldCreateCartesianArgumentsForAggregateWith() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(AggregatedFixtureSourceSpec.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(1).succeeded(1));

        assertEquals(Set.of("a0:b0"), AggregatedFixtureSourceSpec.COMBINATIONS);
        assertEquals(List.of(
                        "create:a0", "load:" + AXIS_A_TEMPLATE,
                        "create:b0", "load:" + AXIS_B_TEMPLATE,
                        "delete:" + AXIS_B_TEMPLATE, "delete:" + AXIS_A_TEMPLATE),
                RecursiveFixtureRegistry.events());
    }

    @Test
    void shouldDelegateFixtureConversionToJUnitConverter() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(ConvertedFixtureSourceSpec.class))
                .execute()
                .testEvents()
                .assertStatistics(statistics -> statistics.started(1).succeeded(1));

        assertEquals(Set.of("a0"), ConvertedFixtureSourceSpec.VALUES);
        assertEquals(List.of(
                        "create:a0", "load:" + AXIS_A_TEMPLATE,
                        "delete:" + AXIS_A_TEMPLATE),
                RecursiveFixtureRegistry.events());
    }

    @Test
    void shouldRejectEachModeWithMoreThanOneIndexedParameter() {
        FixtureArgumentsProvider provider = provider(EACH, template(AXIS_A_TEMPLATE, 1));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> provider.provideArguments(
                        parameters(AxisAFixture.class, AxisAFixture.class),
                        mock(ExtensionContext.class)));

        assertEquals(
                "@FixtureSource mode EACH requires 1 indexed test-method parameter(s), but found 2.",
                exception.getMessage());
        assertTrue(RecursiveFixtureRegistry.events().isEmpty());
    }

    @Test
    void shouldDeletePreparedFixturesWhenArgumentTypeIsInvalid() {
        FixtureArgumentsProvider provider = provider(EACH, template(AXIS_A_TEMPLATE, 1));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> provider.provideArguments(
                        parameters(AxisBFixture.class),
                        mock(ExtensionContext.class)));

        assertTrue(exception.getMessage().contains("cannot be assigned to parameter 0"));
        assertEquals(List.of(
                        "create:a0", "load:" + AXIS_A_TEMPLATE, "delete:" + AXIS_A_TEMPLATE),
                RecursiveFixtureRegistry.events());
    }

    private FixtureArgumentsProvider provider(
            FixtureSourceMode mode,
            Template... templates) {
        FixtureSource source = mock(FixtureSource.class);
        when(source.value()).thenReturn(templates);
        when(source.mode()).thenReturn(mode);
        FixtureArgumentsProvider provider = new FixtureArgumentsProvider();
        provider.accept(source);
        return provider;
    }

    private Template template(String name, int count) {
        Template template = mock(Template.class);
        when(template.name()).thenReturn(name);
        when(template.count()).thenReturn(count);
        return template;
    }

    private ParameterDeclarations parameters(Class<?>... types) {
        List<ParameterDeclaration> declarations = new ArrayList<>(types.length);
        for (int index = 0; index < types.length; index++) {
            ParameterDeclaration declaration = mock(ParameterDeclaration.class);
            doReturn(types[index]).when(declaration).getParameterType();
            when(declaration.getParameterIndex()).thenReturn(index);
            declarations.add(declaration);
        }
        ParameterDeclarations parameters = mock(ParameterDeclarations.class);
        when(parameters.getAll()).thenReturn(List.copyOf(declarations));
        return parameters;
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

class AutoEachFixtureSourceSpec {

    static final Set<String> VALUES = ConcurrentHashMap.newKeySet();

    @ParameterizedTest
    @FixtureSource({
            @Template(name = AXIS_A_TEMPLATE, count = 1),
            @Template(name = AXIS_A_TEMPLATE, count = 1)
    })
    void receivesEveryFixture(AxisAFixture fixture) {
        VALUES.add(fixture.value());
    }
}

class EachFixtureSourceSpec {

    static final Set<String> VALUES = ConcurrentHashMap.newKeySet();

    @ParameterizedTest
    @FixtureSource({
            @Template(name = AXIS_A_TEMPLATE, count = 2),
            @Template(name = AXIS_B_TEMPLATE, count = 3)
    })
    void receivesEveryFixture(Fixture fixture) {
        if (fixture instanceof AxisAFixture axisA) {
            VALUES.add(axisA.value());
        } else if (fixture instanceof AxisBFixture axisB) {
            VALUES.add(axisB.value());
        }
    }
}

class ArgumentsAccessorFixtureSourceSpec {

    static final Set<String> COMBINATIONS = ConcurrentHashMap.newKeySet();

    @ParameterizedTest
    @FixtureSource({
            @Template(name = AXIS_A_TEMPLATE, count = 1),
            @Template(name = AXIS_B_TEMPLATE, count = 1)
    })
    void receivesEveryCombination(ArgumentsAccessor arguments) {
        AxisAFixture first = arguments.get(0, AxisAFixture.class);
        AxisBFixture second = arguments.get(1, AxisBFixture.class);
        COMBINATIONS.add(first.value() + ":" + second.value());
    }
}

class AggregatedFixtureSourceSpec {

    static final Set<String> COMBINATIONS = ConcurrentHashMap.newKeySet();

    @ParameterizedTest
    @FixtureSource({
            @Template(name = AXIS_A_TEMPLATE, count = 1),
            @Template(name = AXIS_B_TEMPLATE, count = 1)
    })
    void receivesEveryCombination(@AggregateWith(FixturePairAggregator.class) FixturePair fixtures) {
        COMBINATIONS.add(fixtures.first().value() + ":" + fixtures.second().value());
    }
}

record FixturePair(AxisAFixture first, AxisBFixture second) {
}

class FixturePairAggregator implements ArgumentsAggregator {

    @Override
    public FixturePair aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
        return new FixturePair(
                arguments.get(0, AxisAFixture.class),
                arguments.get(1, AxisBFixture.class));
    }
}

class ConvertedFixtureSourceSpec {

    static final Set<String> VALUES = ConcurrentHashMap.newKeySet();

    @ParameterizedTest
    @FixtureSource(@Template(name = AXIS_A_TEMPLATE, count = 1))
    void receivesConvertedFixture(@ConvertWith(AxisFixtureValueConverter.class) String value) {
        VALUES.add(value);
    }
}

class AxisFixtureValueConverter implements ArgumentConverter {

    @Override
    public String convert(Object source, ParameterContext context) {
        return ((AxisAFixture) source).value();
    }
}
