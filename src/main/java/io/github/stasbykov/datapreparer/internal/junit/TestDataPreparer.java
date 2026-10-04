package io.github.stasbykov.datapreparer.internal.junit;

import io.github.stasbykov.datapreparer.api.annotation.FixtureReference;
import io.github.stasbykov.datapreparer.api.annotation.Template;
import io.github.stasbykov.datapreparer.api.core.Fixture;
import io.github.stasbykov.datapreparer.api.core.FixtureBatch;
import io.github.stasbykov.datapreparer.api.core.FixtureTemplate;
import io.github.stasbykov.datapreparer.internal.core.FixtureHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Prepares fixture graphs described by {@link Template} annotations.
 */
public class TestDataPreparer {

    private final FixtureHandler fixtureHandler;

    public TestDataPreparer(@NotNull FixtureHandler fixtureHandler) {
        this.fixtureHandler = fixtureHandler;
    }

    /**
     * Loads all requested fixture graphs and returns all their batches.
     */
    public List<FixtureBatch<? extends Fixture>> processTemplatesForLoading(Template[] templates) {
        return prepareFixtures(templates).batches();
    }

    /**
     * Loads fixture graphs, retaining both the complete dependency list and the
     * explicitly requested root batches.
     */
    public FixturePreparation prepareFixtures(Template[] templates) {
        validateTemplate(templates);

        List<FixtureBatch<? extends Fixture>> batches = new ArrayList<>();
        List<FixtureBatch<? extends Fixture>> roots = new ArrayList<>();
        try {
            Arrays.stream(templates)
                    .map(template -> loadTemplate(template, new ArrayDeque<>(), batches))
                    .forEach(roots::add);
            return new FixturePreparation(batches, roots);
        } catch (RuntimeException | Error exception) {
            cleanupAfterFailedPreparation(batches, exception);
            throw exception;
        }
    }

    /**
     * Deletes loaded fixtures in reverse load order, so dependants are removed
     * before their dependencies.
     */
    public static void processTemplatesForDeletion(List<FixtureBatch<? extends Fixture>> fixtures) {
        requireNonNull(fixtures, "Fixture batches cannot be null");
        List<FixtureBatch<? extends Fixture>> reversed = new ArrayList<>(fixtures);
        Collections.reverse(reversed);
        Throwable firstFailure = null;
        for (FixtureBatch<? extends Fixture> batch : reversed) {
            try {
                deleteBatch(batch);
            } catch (RuntimeException | Error exception) {
                if (firstFailure == null) {
                    firstFailure = exception;
                } else {
                    firstFailure.addSuppressed(exception);
                }
            }
        }
        rethrowDeletionFailure(firstFailure);
    }

    private FixtureBatch<? extends Fixture> loadTemplate(
            Template template,
            Deque<String> path,
            List<FixtureBatch<? extends Fixture>> batches) {
        validateTemplate(template);
        FixtureTemplate<? extends Fixture> fixtureTemplate = fixtureHandler.getTemplate(template.name())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Fixture template was not found: " + template.name()));
        return loadTemplateGraph(fixtureTemplate, template.count(), path, batches);
    }

    private FixtureBatch<? extends Fixture> loadReferencedTemplate(
            String templateName,
            int count,
            Deque<String> path,
            List<FixtureBatch<? extends Fixture>> batches) {
        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("The value of @FixtureReference cannot be null or empty.");
        }
        FixtureTemplate<? extends Fixture> template = fixtureHandler.getTemplate(templateName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Referenced fixture template was not found: " + templateName));
        return loadTemplateGraph(template, count, path, batches);
    }

    private <T extends Fixture> FixtureBatch<T> loadTemplateGraph(
            FixtureTemplate<T> template,
            int count,
            Deque<String> path,
            List<FixtureBatch<? extends Fixture>> batches) {
        validateFixtureTemplate(template, template.loader(), "FixtureLoader");
        detectCycle(template.name(), path);
        path.addLast(template.name());
        try {
            List<T> data = IntStream.range(0, count)
                    .mapToObj(index -> template.data().get())
                    .filter(Objects::nonNull)
                    .toList();
            List<T> resolvedData = resolveReferences(data, path, batches);
            List<T> fixtures = requireNonNull(
                    template.loader().load(resolvedData),
                    "FixtureLoader returned null for template: " + template.name());

            FixtureBatch<T> batch = new FixtureBatch<>(template, List.copyOf(fixtures));
            batches.add(batch);
            return batch;
        } finally {
            path.removeLast();
        }
    }

    private <T extends Fixture> List<T> resolveReferences(
            List<T> fixtures,
            Deque<String> path,
            List<FixtureBatch<? extends Fixture>> batches) {
        if (fixtures.isEmpty()) {
            return fixtures;
        }

        Class<?> fixtureType = fixtures.get(0).getClass();
        if (fixtures.stream().anyMatch(fixture -> fixture.getClass() != fixtureType)) {
            throw new IllegalArgumentException("A fixture template must create instances of one concrete type.");
        }

        List<Field> referenceFields = getFixtureReferenceFields(fixtureType);
        if (referenceFields.isEmpty()) {
            return fixtures;
        }

        Map<Field, List<? extends Fixture>> references = new LinkedHashMap<>();
        for (Field field : referenceFields) {
            validateReferenceField(field);
            String referencedTemplateName = field.getAnnotation(FixtureReference.class).value();
            FixtureBatch<? extends Fixture> referencedBatch = loadReferencedTemplate(
                    referencedTemplateName, fixtures.size(), path, batches);
            validateReferenceValues(field, referencedTemplateName, fixtures.size(), referencedBatch.fixtures());
            references.put(field, referencedBatch.fixtures());
        }

        return fixtureType.isRecord()
                ? rebuildRecords(fixtures, fixtureType, references)
                : injectMutableFields(fixtures, references);
    }

    private List<Field> getFixtureReferenceFields(Class<?> fixtureType) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> type = fixtureType; type != null && type != Object.class; type = type.getSuperclass()) {
            Arrays.stream(type.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(FixtureReference.class))
                    .forEach(fields::add);
        }
        return fields;
    }

    private void validateReferenceField(Field field) {
        if (!Fixture.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("Field annotated with @FixtureReference must implement Fixture: "
                    + field.getDeclaringClass().getName() + "." + field.getName());
        }
    }

    private void validateReferenceValues(
            Field field,
            String templateName,
            int expectedCount,
            List<? extends Fixture> values) {
        if (values.size() != expectedCount) {
            throw new IllegalStateException("Referenced template '%s' returned %d fixtures, but %d were required for field %s."
                    .formatted(templateName, values.size(), expectedCount, field.getName()));
        }
        values.stream()
                .filter(Objects::nonNull)
                .filter(value -> !field.getType().isInstance(value))
                .findFirst()
                .ifPresent(value -> {
                    throw new IllegalArgumentException("Referenced template '%s' produced %s, which cannot be assigned to field %s of type %s."
                            .formatted(templateName, value.getClass().getName(), field.getName(), field.getType().getName()));
                });
    }

    private <T extends Fixture> List<T> injectMutableFields(
            List<T> fixtures,
            Map<Field, List<? extends Fixture>> references) {
        try {
            for (Map.Entry<Field, List<? extends Fixture>> entry : references.entrySet()) {
                Field field = entry.getKey();
                field.setAccessible(true);
                for (int index = 0; index < fixtures.size(); index++) {
                    field.set(fixtures.get(index), entry.getValue().get(index));
                }
            }
            return fixtures;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to inject a referenced fixture.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Fixture> List<T> rebuildRecords(
            List<T> fixtures,
            Class<?> fixtureType,
            Map<Field, List<? extends Fixture>> references) {
        RecordComponent[] components = fixtureType.getRecordComponents();
        Class<?>[] parameterTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        try {
            Constructor<?> constructor = fixtureType.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            List<T> rebuilt = new ArrayList<>(fixtures.size());
            for (int fixtureIndex = 0; fixtureIndex < fixtures.size(); fixtureIndex++) {
                T fixture = fixtures.get(fixtureIndex);
                Object[] arguments = new Object[components.length];
                for (int componentIndex = 0; componentIndex < components.length; componentIndex++) {
                    RecordComponent component = components[componentIndex];
                    Field field = fixtureType.getDeclaredField(component.getName());
                    List<? extends Fixture> referencedValues = references.get(field);
                    arguments[componentIndex] = referencedValues == null
                            ? component.getAccessor().invoke(fixture)
                            : referencedValues.get(fixtureIndex);
                }
                rebuilt.add((T) constructor.newInstance(arguments));
            }
            return List.copyOf(rebuilt);
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException
                 | InvocationTargetException | InstantiationException exception) {
            throw new IllegalStateException("Failed to rebuild record fixture: " + fixtureType.getName(), exception);
        }
    }

    private void detectCycle(String templateName, Deque<String> path) {
        if (!path.contains(templateName)) {
            return;
        }
        List<String> cycle = new ArrayList<>(path);
        cycle.add(templateName);
        throw new IllegalStateException("Circular fixture reference detected: " + String.join(" -> ", cycle));
    }

    private static void cleanupAfterFailedPreparation(
            List<FixtureBatch<? extends Fixture>> batches,
            Throwable originalException) {
        List<FixtureBatch<? extends Fixture>> reversed = new ArrayList<>(batches);
        Collections.reverse(reversed);
        for (FixtureBatch<? extends Fixture> batch : reversed) {
            try {
                deleteBatch(batch);
            } catch (RuntimeException | Error cleanupException) {
                originalException.addSuppressed(cleanupException);
            }
        }
    }

    private static <T extends Fixture> void deleteBatch(FixtureBatch<T> batch) {
        FixtureTemplate<T> template = batch.template();
        validateFixtureTemplate(template, template.deleter(), "FixtureDeleter");
        template.deleter().delete(batch.fixtures());
    }

    private static void rethrowDeletionFailure(Throwable failure) {
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }
    }

    private void validateTemplate(Template[] templates) {
        requireNonNull(templates, "The list of templates (@Template) cannot be null");
        Arrays.stream(templates).forEach(this::validateTemplate);
    }

    private void validateTemplate(Template template) {
        requireNonNull(template, "The template parameter annotation cannot be null");
        if (template.name() == null || template.name().isBlank()) {
            throw new IllegalArgumentException("The name parameter of the @Template annotation cannot be null or empty.");
        }
        if (template.count() <= 0) {
            throw new IllegalArgumentException("The count parameter of the @Template annotation cannot be 0 or negative.");
        }
    }

    private static <T extends Fixture, R> void validateFixtureTemplate(
            FixtureTemplate<T> template,
            R component,
            String componentName) {
        requireNonNull(template, "FixtureTemplate cannot be null");
        requireNonNull(template.name(), "Template name cannot be null");
        requireNonNull(template.data(), "Fixture cannot be null in template named:" + template.name());
        requireNonNull(component, componentName + " cannot be null in template named:" + template.name());
    }
}
