package io.github.stasbykov.datapreparer.test.core;

import io.github.stasbykov.datapreparer.api.core.Fixture;
import io.github.stasbykov.datapreparer.api.core.FixtureRegistry;
import io.github.stasbykov.datapreparer.api.core.FixtureTemplate;
import io.github.stasbykov.datapreparer.internal.core.FixtureHandler;
import io.github.stasbykov.datapreparer.internal.util.scanner.ClassScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для проверки работы {@link FixtureHandler}.
 */
@ExtendWith(MockitoExtension.class)
public class FixtureHandlerTest {
    /**
     * Значение параметра для хранения названия пакета с {@link FixtureRegistry} по умолчанию
     */
    private final static String PACKAGE_NAME_PROPERTIES = "fixture.package.registry";

    /**
     * Кастомный пакет для хранения {@link FixtureRegistry}
     */
    private final static String CUSTOM_PACKAGE_REGISTRY = "custom.package";

    /**
     * Название шаблона фикстуры для теста
     */
    private final static String FIXTURE_TEMPLATE_NAME = "name_template";

    @Mock
    private ClassScanner classScanner;

    @Mock
    private FixtureRegistry<Fixture> registry;

    @Mock
    private FixtureTemplate<Fixture> template;

    /**
     * Проверяет успешное получение шаблона.
     */
    @Test
    void shouldSuccessGettingTemplate() {
        System.setProperty(PACKAGE_NAME_PROPERTIES, CUSTOM_PACKAGE_REGISTRY);
        when(classScanner.findAndInstantiate(any(), any())).thenReturn(List.of(registry));
        when(registry.getTemplates()).thenReturn(List.of(template));
        when(template.name()).thenReturn(FIXTURE_TEMPLATE_NAME);

        FixtureHandler handler = new FixtureHandler(classScanner);
        FixtureTemplate<? extends  Fixture> actualTemplate = handler.getTemplate(FIXTURE_TEMPLATE_NAME).orElseGet(null);

        assertAll(
                () -> assertEquals(template, actualTemplate, "Полученный шаблон не соответствует ожидаемому"),
                () -> verify(classScanner, times(1)).findAndInstantiate(FixtureRegistry.class, CUSTOM_PACKAGE_REGISTRY)
        ); // Проверяем корректный проброс системной настройки

        System.clearProperty(PACKAGE_NAME_PROPERTIES);

    }

    /**
     * Проверяет возврат пустого результата при пустом списке реестров.
     */
    @Test
    void shouldEmptyResultByEmptyRegistryList() {
        when(classScanner.findAndInstantiate(any(), any())).thenReturn(List.of());

        FixtureHandler handler = new FixtureHandler(classScanner);
        Optional<FixtureTemplate<? extends  Fixture>> actualTemplate = handler.getTemplate(FIXTURE_TEMPLATE_NAME);

        assertAll(
                () -> assertFalse(actualTemplate.isPresent(), "Должен быть возвращен пустой результат"),
                () -> verify(classScanner, times(1)).findAndInstantiate(FixtureRegistry.class, "")
        ); // Проверяем, что системная настройка не передается, т.к. мы не устанавливали соответствующее свойство

    }

    /**
     * Проверяет возврат пустого результата при отсутствующем имени шаблона.
     */
    @Test
    void shouldEmptyResultByNonExistentNameTemplate() {
        String NON_EXISTENT_TEMPLATE_NAME = "non_existent";
        when(classScanner.findAndInstantiate(any(), any())).thenReturn(List.of(registry));
        when(registry.getTemplates()).thenReturn(List.of(template));
        when(template.name()).thenReturn(FIXTURE_TEMPLATE_NAME);

        FixtureHandler handler = new FixtureHandler(classScanner);
        Optional<FixtureTemplate<? extends  Fixture>> actualTemplate = handler.getTemplate(NON_EXISTENT_TEMPLATE_NAME);

        assertAll(
                () -> assertFalse(actualTemplate.isPresent(), "Должен быть возвращен пустой результат"),
                () -> verify(classScanner, times(1)).findAndInstantiate(FixtureRegistry.class, "")
        ); // Проверяем, что системная настройка не передается, т.к. мы не устанавливали соответствующее свойство
    }

    /**
     * Проверяет выброс исключения при передаче null вместо имени шаблона.
     */
    @Test
    void shouldThrowNullPointerExceptionForNullNameTemplate() {
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> new FixtureHandler(classScanner).getTemplate(null));

        assertEquals("Template name can`t be null", thrown.getMessage());

    }

}
