package io.github.stasbykov.datapreparer.internal.junit;

import io.github.stasbykov.datapreparer.api.annotation.Template;
import io.github.stasbykov.datapreparer.api.core.Fixture;
import io.github.stasbykov.datapreparer.api.core.FixtureBatch;
import io.github.stasbykov.datapreparer.api.core.FixtureLoader;
import io.github.stasbykov.datapreparer.api.core.FixtureTemplate;
import io.github.stasbykov.datapreparer.internal.core.FixtureHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Class for preparing test data using {@link Template} annotations.
 *
 * @see FixtureHandler
 * @since 1.0.0
 */
public class TestDataPreparer {

    private final FixtureHandler fixtureHandler;

    public TestDataPreparer(@NotNull FixtureHandler fixtureHandler) {
        this.fixtureHandler = fixtureHandler;
    }

    /**
     * Processes an array of {@link Template} annotations to load fixtures.
     *
     * @param templates array of annotations {@link Template}
     * @return list of fixture batches containing the template name and a list of fixtures
     */
    public List<FixtureBatch<? extends Fixture>> processTemplatesForLoading(Template[] templates) {
        validateTemplate(templates);
        return Arrays.stream(templates)
                .map(this::loadTemplate)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Processes a list of fixture packages to remove.
     *
     * @param fixtures list of fixture batches containing the template name and a list of fixtures
     */
    public static void processTemplatesForDeletion(List<FixtureBatch<? extends Fixture>> fixtures) {
        fixtures.forEach(TestDataPreparer::deleteBatch);
    }

    /**
     * Loads a fixture template based on the {@link Template} annotation and using {@link FixtureHandler}
     *
     * @param template template fixture {@link Template}
     * @return Optional of batch fixtures containing the template name and a list of fixtures
     */
    private Optional<FixtureBatch<? extends Fixture>> loadTemplate(Template template) {
        validateTemplate(template);

        return fixtureHandler.getTemplate(template.name())
                .map(tmpl -> loadTemplateWithCount(tmpl, template.count()));
    }

    /**
     * Loads a fixture template with the given number of instances.
     *
     * @param template fixture template
     * @param count  number of fixture instances
     * @param <T>  fixture type
     * @return batch fixtures containing the template name and a list of fixtures
     */
    private <T extends Fixture> FixtureBatch<T> loadTemplateWithCount(FixtureTemplate<T> template, int count) {
        validateFixtureTemplate(template, template.loader(), "FixtureLoader");

        List<T> data = IntStream.range(0, count)
                .mapToObj(i -> template.data().get())
                .filter(Objects::nonNull)
                .toList();

        List<T> fixtures = template.loader().load(data);

        return new FixtureBatch<>(template, fixtures);
    }

    /**
     * Removes a fixture package.
     *
     * @param batch batch fixtures
     * @param <T> type of fixture
     */
    private static <T extends Fixture> void deleteBatch(FixtureBatch<T> batch) {
        FixtureTemplate<T> template = batch.template();
        validateFixtureTemplate(template, template.deleter(), "FixtureDeleter");

        template.deleter().delete(batch.fixtures());
    }

    /**
     * Validates the passed {@link Template} annotations.
     *
     * @param templates is an array of annotations {@link Template}
     * @throws NullPointerException if the list of templates or any of the templates is null
     * @throws IllegalArgumentException if any of the templates fail validation
     * @see #validateTemplate(Template) for validating an individual template.
     */
    private void validateTemplate(Template[] templates) {
        requireNonNull(templates, "The list of templates (@Template) cannot be null");
        Arrays.stream(templates).forEach(this::validateTemplate);
    }

    /**
     * Validates the passed {@link Template} annotation.
     *
     * @param template annotation {@link Template}
     * @throws NullPointerException if the template is null
     * @throws IllegalArgumentException if the template fails validation
     */
    private void validateTemplate(Template template) {
        requireNonNull(template, "The template parameter annotation cannot be null");

        if (template.name() == null || template.name().isBlank()) {
            throw new IllegalArgumentException("The name parameter of the @Template annotation cannot be null or empty.");
        }
        if (template.count() <= 0) {
            throw new IllegalArgumentException("The count parameter of the @Template annotation cannot be 0 or negative.");
        }
    }

    /**
     * Validation for all template operations.
     *
     * @param template fixture template
     * @param component operation component (loader/deleter)
     * @param componentName component name for error message
     * @param <T> fixture type
     * @param <R> component type (any)
     */
    private static <T extends Fixture, R> void validateFixtureTemplate(FixtureTemplate<T> template, R component, String componentName) {
        requireNonNull(template, "FixtureTemplate cannot be null");
        requireNonNull(template.name(), "Template name cannot be null");
        requireNonNull(template.data(), "Fixture cannot be null in template named:" + template.name());
        requireNonNull(component, componentName + " cannot be null in template named:" + template.name());
    }
}
