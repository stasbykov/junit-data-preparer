package io.github.stasbykov.datapreparer.internal.util.scanner;

import java.util.List;

/**
 * Defines basic functionality for class scanner.
 *
 * @since 1.0.0
 */
public interface ClassScanner {

    /**
     * Finds and creates instances of all non-abstract classes
     * implementing the specified interface in the package from the system property.
     *
     * @param interfaceClass  the interface to search for
     * @param packageName  the package name (e.g., "app.some.package") to search for classes
     * @param <T>  the interface type
     * @return the list of created instances
     */
    <T> List<T> findAndInstantiate(Class<T> interfaceClass, String packageName);
}