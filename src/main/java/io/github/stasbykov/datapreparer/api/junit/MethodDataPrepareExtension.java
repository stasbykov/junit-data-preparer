package io.github.stasbykov.datapreparer.api.junit;

import io.github.stasbykov.datapreparer.api.annotation.MethodDataSetup;
import io.github.stasbykov.datapreparer.api.core.FixtureBatchCollection;
import io.github.stasbykov.datapreparer.internal.junit.PrepareExtensionManager;
import io.github.stasbykov.datapreparer.internal.util.scanner.ClassgraphScanner;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * A JUnit extension that allows you to automatically load fixtures before executing test methods annotated with {@link MethodDataSetup}.
 *
 * @since 1.0.0
 */
public class MethodDataPrepareExtension implements ParameterResolver {

    private final PrepareExtensionManager prepareExtensionManager;
    private final Logger logger;

    /**
     * A default constructor that initializes the object using Classgraph to scan fixture interfaces.
     */
    public MethodDataPrepareExtension() {
        this(new PrepareExtensionManager(new ClassgraphScanner(),
                        ExtensionContext.Namespace.create(MethodDataPrepareExtension.class)),
                LoggerFactory.getLogger(MethodDataPrepareExtension.class));
    }

    /**
     * Package-private constructor for tests
     *
     */
    private MethodDataPrepareExtension(PrepareExtensionManager prepareExtensionManager, Logger logger) {
        this.prepareExtensionManager = prepareExtensionManager;
        this.logger = logger;

    }

    /**
     * Checks if the parameter is supported by the current extension.
     *
     * @param parameterContext  JUnit parameter context
     * @param extensionContext  JUnit extension context
     * @return true if the parameter is annotated with {@link MethodDataSetup} and is of type {@link FixtureBatchCollection}, otherwise false
     * @throws ParameterResolutionException if an error occurs while validating the parameter
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.isAnnotated(MethodDataSetup.class) && parameterContext.getParameter().getType().equals(FixtureBatchCollection.class);
    }

    /**
     * Resolves the parameter for the test method.
     *
     * @param parameterContext  JUnit parameter context
     * @param extensionContext  JUnit extension context
     * @return prepared fixture data as {@link FixtureBatchCollection}
     * @throws ParameterResolutionException if an error occurs while resolving the parameter
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        logger.info("Start of the method of preparing fixtures before testing {}", extensionContext.getTestMethod().map(Method::getName).orElse("Undefined"));
        return prepareExtensionManager.computeValueOnce(parameterContext, extensionContext);
    }
}
