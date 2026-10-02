package io.github.stasbykov.datapreparer.test.junit.extension.fixture;

import io.github.stasbykov.datapreparer.api.annotation.FixtureReference;
import io.github.stasbykov.datapreparer.api.core.Fixture;
import io.github.stasbykov.datapreparer.api.core.FixtureRegistry;
import io.github.stasbykov.datapreparer.api.core.FixtureTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecursiveFixtureRegistry implements FixtureRegistry<Fixture> {

    public static final String COMPANY_TEMPLATE = "recursive_company";
    public static final String USER_TEMPLATE = "recursive_user";
    public static final String ORDER_TEMPLATE = "recursive_order";
    public static final String CYCLE_A_TEMPLATE = "recursive_cycle_a";
    public static final String CYCLE_B_TEMPLATE = "recursive_cycle_b";
    public static final String AXIS_A_TEMPLATE = "cartesian_axis_a";
    public static final String AXIS_B_TEMPLATE = "cartesian_axis_b";

    private static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

    @Override
    public List<FixtureTemplate<Fixture>> getTemplates() {
        return List.of(
                template(COMPANY_TEMPLATE,
                        () -> new CompanyFixture("company"),
                        fixtures -> fixtures.stream()
                                .map(CompanyFixture.class::cast)
                                .map(company -> new CompanyFixture("loaded-" + company.name()))
                                .map(Fixture.class::cast)
                                .toList()),
                template(USER_TEMPLATE,
                        () -> new UserFixture("user", null),
                        fixtures -> fixtures.stream()
                                .map(UserFixture.class::cast)
                                .map(user -> new UserFixture("loaded-" + user.name(), user.company()))
                                .map(Fixture.class::cast)
                                .toList()),
                template(ORDER_TEMPLATE,
                        () -> new OrderFixture("order", null),
                        fixtures -> fixtures.stream()
                                .map(OrderFixture.class::cast)
                                .map(order -> new OrderFixture("loaded-" + order.number(), order.user()))
                                .map(Fixture.class::cast)
                                .toList()),
                template(CYCLE_A_TEMPLATE, () -> new CycleAFixture(null), List::copyOf),
                template(CYCLE_B_TEMPLATE, () -> new CycleBFixture(null), List::copyOf),
                template(AXIS_A_TEMPLATE, () -> new AxisAFixture(nextValue("a")), List::copyOf),
                template(AXIS_B_TEMPLATE, () -> new AxisBFixture(nextValue("b")), List::copyOf)
        );
    }

    public static void clearEvents() {
        EVENTS.clear();
    }

    public static List<String> events() {
        return List.copyOf(EVENTS);
    }

    private static String nextValue(String prefix) {
        long count = EVENTS.stream().filter(event -> event.startsWith("create:" + prefix)).count();
        String value = prefix + count;
        EVENTS.add("create:" + value);
        return value;
    }

    private FixtureTemplate<Fixture> template(
            String name,
            java.util.function.Supplier<Fixture> supplier,
            java.util.function.Function<List<Fixture>, List<Fixture>> loader) {
        return new FixtureTemplate<>(
                name,
                fixtures -> {
                    EVENTS.add("load:" + name);
                    return loader.apply(fixtures);
                },
                fixtures -> EVENTS.add("delete:" + name),
                supplier);
    }

    public record CompanyFixture(String name) implements Fixture {}

    public record UserFixture(
            String name,
            @FixtureReference(COMPANY_TEMPLATE) CompanyFixture company) implements Fixture {}

    public record OrderFixture(
            String number,
            @FixtureReference(USER_TEMPLATE) UserFixture user) implements Fixture {}

    public record CycleAFixture(
            @FixtureReference(CYCLE_B_TEMPLATE) CycleBFixture fixture) implements Fixture {}

    public record CycleBFixture(
            @FixtureReference(CYCLE_A_TEMPLATE) CycleAFixture fixture) implements Fixture {}

    public record AxisAFixture(String value) implements Fixture {}

    public record AxisBFixture(String value) implements Fixture {}
}
