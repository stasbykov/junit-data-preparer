module io.github.stasbykov.datapreparer.test {

    requires io.github.stasbykov.datapreparer;

    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;
    requires org.mockito;
    requires org.mockito.junit.jupiter;
    requires org.junit.platform.testkit;
    requires org.junit.platform.engine;

    opens io.github.stasbykov.datapreparer.test.core;
    opens io.github.stasbykov.datapreparer.test.junit.extension;

    exports io.github.stasbykov.datapreparer.test.junit.extension.fixture
            to io.github.stasbykov.datapreparer;

    exports io.github.stasbykov.datapreparer.test.core
            to io.github.stasbykov.datapreparer;

    opens io.github.stasbykov.datapreparer.test.junit.extension.fixture
            to io.github.stasbykov.datapreparer;
}