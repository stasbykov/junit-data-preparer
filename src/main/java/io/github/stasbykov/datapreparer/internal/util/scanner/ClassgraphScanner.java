package io.github.stasbykov.datapreparer.internal.util.scanner;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of a scanner based on the {@code io.github.classgraph:classgraph} library.
 *
 * @since 1.0.0
 */
public class ClassgraphScanner implements ClassScanner {

    private final Logger log;

    public ClassgraphScanner() {
        this.log = LoggerFactory.getLogger(ClassScanner.class);
    }

    public ClassgraphScanner(Logger logger) {
        this.log = logger;
    }

    /**
     * Finds and creates instances of all non-abstract classes
     * implementing the specified interface in the package from the system property.
     *
     * @param interfaceClass the interface to search for
     * @param packageName the package name (e.g., "app.some.package") to search for classes
     * @param <T> the interface type
     * @return the list of created instances
     */
    @Override
    public <T> List<T> findAndInstantiate(Class<T> interfaceClass, String packageName) {

        ClassGraph classGraph = new ClassGraph()
                .enableAllInfo()
                .ignoreClassVisibility();

        Optional.ofNullable(packageName)
                .map(String::trim)
                .filter(s -> !s.isBlank() )
                .ifPresent(classGraph::acceptPackages);

        try (ScanResult scanResult = classGraph
                .scan()) {

            return scanResult.getClassesImplementing(interfaceClass.getName())
                    .stream()
                    .filter(classInfo -> !classInfo.isAbstract() && !classInfo.isInterface())
                    .map(classInfo -> {
                        try {
                            @SuppressWarnings("unchecked")
                            Class<T> clazz = (Class<T>) classInfo.loadClass();
                            return clazz.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            log.warn("Failed to create instance {}", classInfo.getName(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new IllegalStateException("Packet scan error '" + packageName + "'", e);
        }
    }
}
