module io.github.stasbykov.datapreparer {
    requires org.junit.jupiter.api;
    requires org.slf4j;
    requires org.jetbrains.annotations;
    requires io.github.classgraph;

    exports io.github.stasbykov.datapreparer.api.annotation;
    exports io.github.stasbykov.datapreparer.api.core;
    exports io.github.stasbykov.datapreparer.api.junit;

    exports io.github.stasbykov.datapreparer.internal.core to
            io.github.stasbykov.datapreparer.test;

    exports io.github.stasbykov.datapreparer.internal.util.junit to
            io.github.stasbykov.datapreparer.test;

    exports io.github.stasbykov.datapreparer.internal.util.scanner to
            io.github.stasbykov.datapreparer.test;

}