# JUnit Data Preparer

[![JUnit5](https://img.shields.io/badge/JUnit5-26A65B?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Java](https://img.shields.io/badge/Java-ED8B00?logo=java&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apache-maven&logoColor=white)](https://maven.apache.org/)

A JUnit5 library extension for preparing data before tests. The library provides interfaces for working with test data (
creating it before tests and deleting it after).

## Features

- Annotations for data preparation before testing
    - @ClassDataSetup to prepare data for the entire test class
    - @MethodDataSetup for preparing data for a specific test method
- FixtureLoader and FixtureDeleter interfaces that are integrated into the JUnit5 framework lifecycle and are
  automatically executed before and after tests
- Templates and template registries for creating dynamic test data
- Recursive fixture references with circular-reference detection
- Cartesian fixture argument sources for JUnit parameterized tests

## Requirements

- Java 17 or higher
- JUnit 5.0.x or higher

## Usage

### Adding a library to the project

- for Maven:

  ```xml

  <dependency>
    <groupId>io.github.stasbykov</groupId>
    <artifactId>junit-data-preparer</artifactId>
    <!-- See the current version in maven -->
    <version>2.0.0</version>
    <scope>test</scope>
  </dependency>

  ```

- for Gradle:

  ```groovy

  // See the current version in maven
  testImplementation 'io.github.stasbykov:junit-data-preparer:2.0.0'
  
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

### Using fixtures in parameterized tests

`@FixtureSource` supplies one test-method argument from each listed template. If templates produce different sets
of fixtures, every possible combination is executed (Cartesian product).

Fixtures are loaded once before the parameterized invocations and are shared by all generated argument combinations.
After every invocation of the parameterized method has finished, each loaded fixture batch is deleted once. Deletion
also runs if an invocation fails and uses reverse load order: containing fixtures are deleted before their referenced
dependencies. Fixtures are not deleted between individual invocations.

```java
import io.github.stasbykov.datapreparer.api.annotation.FixtureSource;
import org.junit.jupiter.params.ParameterizedTest;

class OrderParameterizedTest {

    @ParameterizedTest
    @FixtureSource({
            @Template(name = "first_user_template", count = 2),
            @Template(name = "first_order_template", count = 3)
    })
    void testEveryUserWithEveryOrder(UserFixture user, OrderFixture order) {
        // Executed 2 x 3 = 6 times.
    }
}
```
