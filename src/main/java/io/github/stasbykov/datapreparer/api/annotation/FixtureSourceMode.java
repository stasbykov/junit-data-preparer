package io.github.stasbykov.datapreparer.api.annotation;

/**
 * Defines how {@link FixtureSource} converts loaded fixtures into parameterized-test arguments.
 *
 * @since 2.1.0
 */
public enum FixtureSourceMode {

    /**
     * Supplies every loaded fixture as a separate invocation when the test declares one indexed parameter
     * and no argument aggregator; otherwise uses {@link #CARTESIAN}.
     */
    AUTO,

    /**
     * Supplies every loaded root fixture as a separate test invocation with one fixture argument.
     */
    EACH,

    /**
     * Treats every template as an argument axis and supplies the Cartesian product of all axes.
     */
    CARTESIAN
}
