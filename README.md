# JUnit Data Preparer

[![CI](https://github.com/stasbykov/junit-data-preparer/actions/workflows/ci.yml/badge.svg)](https://github.com/stasbykov/junit-data-preparer/actions/workflows/ci.yml)
[![JUnit 6](https://img.shields.io/badge/JUnit_6-26A65B?logo=junit5&logoColor=white)](https://junit.org/)
[![Java](https://img.shields.io/badge/Java-ED8B00?logo=java&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apache-maven&logoColor=white)](https://maven.apache.org/)

A JUnit library extension for preparing data before tests. The library provides interfaces for working with test data (
creating it before tests and deleting it after).

## Features

- Annotations for data preparation before testing
    - @ClassDataSetup to prepare data for the entire test class
    - @MethodDataSetup for preparing data for a specific test method
- FixtureLoader and FixtureDeleter interfaces that are integrated into the JUnit5 framework lifecycle and are
  automatically executed before and after tests
- Templates and template registries for creating dynamic test data
- Recursive fixture references with circular-reference detection
- Per-fixture and Cartesian argument sources for JUnit parameterized tests

## Requirements

- Java 21 or higher
- JUnit 6.1 or higher

JUnit 5 is not part of the supported compatibility matrix for version 2.x.

## Usage

### Adding a library to the project

- for Maven:

  ```xml

  <dependency>
    <groupId>io.github.stasbykov</groupId>
    <artifactId>junit-data-preparer</artifactId>
    <!-- See the current version in maven -->
    <version>2.1.0</version>
    <scope>test</scope>
  </dependency>

  ```

- for Gradle:

  ```groovy

  // See the current version in maven
  testImplementation 'io.github.stasbykov:junit-data-preparer:2.1.0'
  
  ```

> [!IMPORTANT]
> By default, registry scanning is performed across the entire project (all packages), but you can restrict the scan to
> a specific package by setting the fixture.package.registry parameter in pom.xml, gradle.properties, or -D in the CLI.  
> Example: fixture.package.registry = org.company.somepackage

### Create a test data model

```java

import java.util.UUID;
import io.github.stasbykov.datapreparer.api.annotation.FixtureReference;

public record UserFixture(String name, String age) implements Fixture {
}

public record OrderFixture(
        UUID id,
        Integer sum,
        @FixtureReference("first_user_template") UserFixture user
) implements Fixture {
}

```

A fixture can reference another named fixture template. References are loaded recursively, from the deepest
dependency to the root fixture. Records are supported and are rebuilt with the loaded referenced values.
The referenced template produces one fixture for every containing fixture. Circular reference chains are rejected
with an exception that contains the complete template path. Fixtures are deleted in reverse load order: containing
fixtures first, then their dependencies.

### Create a loader and deleter for this test data model

- FixtureLoader

  ```java

  import io.github.stasbykov.datapreparer.api.core.FixtureLoader;

  public class UserLoader implements FixtureLoader<UserFixture> {
      @Override
      List<UserFixture> load(List<UserFixture> fixtures) {
          // The algorithm for generating test data. This could be an API, a Database, or something else
      
        return fixtures;
      }
  }

  ```

  ```java

  import io.github.stasbykov.datapreparer.api.core.FixtureLoader;

  public class OrderLoader implements FixtureLoader<OrderFixture> {
      @Override
      List<OrderFixture> load(List<OrderFixture> fixtures) {
          // The algorithm for generating test data. This could be an API, a Database, or something else
      
        return fixtures;
      }
  }

  ```

- FixtureDeleter

    ```java

  import io.github.stasbykov.datapreparer.api.core.FixtureDeleter;

  public class UserDeleter implements FixtureDeleter<UserFixture> {
      @Override
      void delete(List<UserFixture> fixtures) {
          // The algorithm for deleting test data. This could be an API, a Database, or something else
      }
  }

  ```

  ```java

  import io.github.stasbykov.datapreparer.api.core.FixtureDeleter;

  public class OrderDeleter implements FixtureDeleter<OrderFixture> {
      @Override
      void load(List<OrderFixture> fixtures) {
          // The algorithm for deleting test data. This could be an API, a Database, or something else
      }
  }

  ```

### Create a Fixture registry

> [!IMPORTANT]
> Template names must be unique.

```java

import io.github.stasbykov.datapreparer.api.core.FixtureRegistry;
import io.github.stasbykov.datapreparer.api.core.FixtureTemplate;

import java.util.List;

public class UserFixtureRegistry implements FixtureRegistry<UserFixture> {
    @Override
    List<FixtureTemplate<UserFixture>> getTemplates() {
        return List.of(
                new FixtureTemplate<UserFixture>(
                        "first_user_template",
                        new UserLoader(),
                        new UserDeleter(),
                        () -> new UserFixture("John", "21")
                ),
                new FixtureTemplate<UserFixture>(
                        "second_user_template",
                        new UserLoader(),
                        new UserDeleter(),
                        () -> new UserFixture("Michael", "25")
                )
        );
    }
}

```

```java

import io.github.stasbykov.datapreparer.api.core.FixtureRegistry;
import io.github.stasbykov.datapreparer.api.core.FixtureTemplate;

import java.util.List;
import java.util.UUID;

public class OrderFixtureRegistry implements FixtureRegistry<OrderFixture> {
    @Override
    List<FixtureTemplate<OrderFixture>> getTemplates() {
        return List.of(
                new FixtureTemplate<OrderFixture>(
                        "first_order_template",
                        new OrderLoader(),
                        new OrderDeleter(),
                        () -> new OrderFixture(UUID.randomUUID(), 100, null)
                ),
                new FixtureTemplate<OrderFixture>(
                        "second_order_template",
                        new OrderLoader(),
                        new OrderDeleter(),
                        () -> new OrderFixture(UUID.randomUUID(), 300, null)
                )
        );
    }
}

```

Now everything is ready to use it in tests.

### Using it in tests

```java

import io.github.stasbykov.datapreparer.api.annotation.ClassDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.FixtureInject;
import io.github.stasbykov.datapreparer.api.annotation.MethodDataSetup;
import io.github.stasbykov.datapreparer.api.annotation.Template;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;

@ClassDataSetup(value = {
        @Template(name = "first_user_template", count = 3),
        @Template(name = "first_order_template", count = 5)},
        inject = true // value false is default
)
public class SomeTestClass {
    @FixtureInject
    FixtureBatchCollection loadedFixtures;

    void someTest() {
        // test
    }

    void secondSomeTest(@MethodDataSetup({
            @Template(name = "second_user_template", count = 1), @Template(name = "second_order_template", count = 2)}
    ) FixtureBatchCollection methodLoadedFixtures) {
        // test
    }
}

```

Class-level fixtures are loaded before JUnit invokes `@BeforeAll`, shared by every test in the class, and deleted
after the class lifecycle completes. With `inject = true`, exactly one `FixtureBatchCollection` field must be marked
with `@FixtureInject`; that field may also be declared in a superclass.

Method-level fixtures are scoped to the individual test invocation. A test method may declare multiple
`@MethodDataSetup` parameters, each with an independent collection of templates. Every requested template name must
exist in a fixture registry. During cleanup, deletion is attempted for every loaded batch in reverse order even if a
deleter fails; subsequent failures are attached to the first exception as suppressed exceptions.

### Using fixtures in parameterized tests

`@FixtureSource` uses `AUTO` mode by default. A test with one indexed parameter and no argument aggregator is invoked
once for every fixture loaded from the listed templates. A test with multiple indexed parameters or an
`ArgumentsAccessor`/`@AggregateWith` parameter treats every template as an argument axis and executes the Cartesian
product. Use `EACH` or `CARTESIAN` to select either behavior explicitly. Standard JUnit argument converters declared
with `@ConvertWith` are applied after the fixture arguments are created.

All declared fixture batches are prepared before the parameterized invocations. After every invocation of the
parameterized method has finished, each loaded fixture batch is deleted once. Deletion also runs if an invocation
fails and uses reverse load order: containing fixtures are deleted before their referenced dependencies. Fixtures
are not deleted between individual invocations.

The following test is executed twice, once for each fixture. Repeating the same template declaration also creates
independent batches, while one declaration with `count = 2` creates a single batch containing two fixtures.

```java
import io.github.stasbykov.datapreparer.api.annotation.FixtureSource;
import org.junit.jupiter.params.ParameterizedTest;

class UserParameterizedTest {

    @ParameterizedTest
    @FixtureSource({
            @Template(name = "active_user_template", count = 1),
            @Template(name = "blocked_user_template", count = 1)
    })
    void testEachUser(UserFixture user) {
        // Executed twice.
    }
}
```

Cartesian mode is useful when every combination of independent fixture sets must be verified.

```java
import io.github.stasbykov.datapreparer.api.annotation.FixtureSource;
import io.github.stasbykov.datapreparer.api.annotation.FixtureSourceMode;
import org.junit.jupiter.params.ParameterizedTest;

class OrderParameterizedTest {

    @ParameterizedTest
    @FixtureSource(
            value = {
                    @Template(name = "first_user_template", count = 2),
                    @Template(name = "first_order_template", count = 3)
            },
            mode = FixtureSourceMode.CARTESIAN)
    void testEveryUserWithEveryOrder(UserFixture user, OrderFixture order) {
        // Executed 2 x 3 = 6 times.
    }
}
```

#### JUnit argument aggregation and conversion

`@FixtureSource` supports the standard JUnit parameterized-test argument mechanisms:

- `ArgumentsAccessor` provides indexed access to the complete argument set for an invocation;
- `@AggregateWith` converts the complete argument set into one reusable, strongly typed object;
- `@ConvertWith` converts one fixture argument to the type declared by the test-method parameter.

For example, the same Cartesian fixture arguments can be consumed directly or aggregated:

```java
void testWithAccessor(ArgumentsAccessor arguments) {
    UserFixture user = arguments.get(0, UserFixture.class);
    OrderFixture order = arguments.get(1, OrderFixture.class);
}

void testWithAggregator(
        @AggregateWith(OrderScenarioAggregator.class) OrderScenario scenario) {
}
```

A converter can expose only the value needed by a test:

```java
void testWithConverter(
        @ConvertWith(UserIdConverter.class) UUID userId) {
}
```

In `AUTO` mode, the presence of `ArgumentsAccessor` or `@AggregateWith` selects `CARTESIAN`, because an aggregator
consumes the complete argument set. `@ConvertWith` remains an indexed parameter, so a single converted parameter
selects `EACH` in the same way as a regular single parameter.
