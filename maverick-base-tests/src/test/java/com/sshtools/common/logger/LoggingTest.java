package com.sshtools.common.logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sshtools.common.logger.Log.Level;

/**
 * Unit tests for the maverick-logging module.
 *
 * We use a {@link ConsoleLoggingContext} as both the default context and current
 * thread context to avoid the heavyweight {@link DefaultLoggerContext} (which reads
 * files and starts background threads) in unit tests.
 */
public class LoggingTest {

    private RootLoggerContext savedDefaultContext;

    @Before
    public void setUp() {
        // Save existing default context so we can restore it after each test
        savedDefaultContext = Log.getDefaultContext();
        // Install a lightweight console context at INFO level as the default
        Log.setDefaultContext(new SimpleTestRootContext(Level.INFO));
        // Make sure no thread-local context is set
        Log.clearCurrentContext();
    }

    @After
    public void tearDown() {
        Log.clearCurrentContext();
        Log.setDefaultContext(savedDefaultContext);
    }

    // -----------------------------------------------------------------------
    // Level checks on default context
    // -----------------------------------------------------------------------

    @Test
    public void testIsInfoEnabledDefault() {
        assertTrue("INFO should be enabled at INFO level", Log.isInfoEnabled());
    }

    @Test
    public void testIsWarnEnabledDefault() {
        assertTrue("WARN should be enabled at INFO level", Log.isWarnEnabled());
    }

    @Test
    public void testIsErrorEnabledDefault() {
        assertTrue("ERROR should be enabled at INFO level", Log.isErrorEnabled());
    }

    @Test
    public void testIsDebugNotEnabledAtInfoLevel() {
        assertFalse("DEBUG should NOT be enabled at INFO level", Log.isDebugEnabled());
    }

    @Test
    public void testIsTraceNotEnabledAtInfoLevel() {
        assertFalse("TRACE should NOT be enabled at INFO level", Log.isTraceEnabled());
    }

    // -----------------------------------------------------------------------
    // Log methods do not throw
    // -----------------------------------------------------------------------

    @Test
    public void testInfoDoesNotThrow() {
        Log.info("test message at INFO level");
    }

    @Test
    public void testWarnDoesNotThrow() {
        Log.warn("test warning");
    }

    @Test
    public void testErrorDoesNotThrow() {
        Log.error("test error");
    }

    @Test
    public void testDebugDoesNotThrow() {
        Log.debug("debug not enabled but call should not throw");
    }

    @Test
    public void testInfoWithExceptionDoesNotThrow() {
        Log.info("info with exception", new RuntimeException("test"));
    }

    // -----------------------------------------------------------------------
    // setDefaultContext
    // -----------------------------------------------------------------------

    @Test
    public void testSetDefaultContext() {
        SimpleTestRootContext ctx = new SimpleTestRootContext(Level.DEBUG);
        Log.setDefaultContext(ctx);
        assertSame(ctx, Log.getDefaultContext());
        assertTrue("DEBUG should be enabled after installing DEBUG context", Log.isDebugEnabled());
    }

    @Test
    public void testSetDefaultContextNoneLevel() {
        Log.setDefaultContext(new SimpleTestRootContext(Level.NONE));
        assertFalse("Nothing should be logged at NONE level", Log.isInfoEnabled());
    }

    // -----------------------------------------------------------------------
    // Thread-local context (setupCurrentContext / clearCurrentContext)
    // -----------------------------------------------------------------------

    @Test
    public void testCurrentContextOverridesDefault() {
        // Default context is INFO level
        assertFalse("DEBUG should be off via default", Log.isDebugEnabled());

        // Set thread-local context at DEBUG level
        Log.setupCurrentContext(new ConsoleLoggingContext(Level.DEBUG));
        assertTrue("DEBUG should be enabled via thread-local context", Log.isDebugEnabled());

        Log.clearCurrentContext();
        assertFalse("DEBUG should be off again after clearing", Log.isDebugEnabled());
    }

    @Test
    public void testClearCurrentContextRestoresDefault() {
        Log.setupCurrentContext(new ConsoleLoggingContext(Level.TRACE));
        assertTrue(Log.isTraceEnabled());
        Log.clearCurrentContext();
        assertFalse(Log.isTraceEnabled()); // default is INFO, no TRACE
    }

    // -----------------------------------------------------------------------
    // ConsoleLoggingContext
    // -----------------------------------------------------------------------

    @Test
    public void testConsoleContextIsLoggingAtLevel() {
        ConsoleLoggingContext ctx = new ConsoleLoggingContext(Level.DEBUG);
        assertTrue(ctx.isLogging(Level.DEBUG));
        assertTrue(ctx.isLogging(Level.INFO));
        assertTrue(ctx.isLogging(Level.WARN));
        assertFalse(ctx.isLogging(Level.TRACE)); // DEBUG ordinal < TRACE ordinal
    }

    @Test
    public void testConsoleContextWritesToSystemOut() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        try {
            ConsoleLoggingContext ctx = new ConsoleLoggingContext(Level.INFO);
            ctx.log(Level.INFO, "hello logging test", null);
            System.out.flush();
        } finally {
            System.setOut(original);
        }
        String output = buf.toString();
        assertTrue("Output should contain the log message", output.contains("hello logging test"));
    }

    @Test
    public void testConsoleContextSuppressesLowerLevel() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        try {
            ConsoleLoggingContext ctx = new ConsoleLoggingContext(Level.WARN);
            ctx.log(Level.DEBUG, "this should not appear", null);
            System.out.flush();
        } finally {
            System.setOut(original);
        }
        String output = buf.toString();
        assertFalse("DEBUG should be suppressed at WARN level", output.contains("this should not appear"));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Minimal {@link RootLoggerContext} implementation for test isolation.
     * Wraps a {@link ConsoleLoggingContext} and forwards all calls to it.
     */
    private static final class SimpleTestRootContext implements RootLoggerContext {

        private final ConsoleLoggingContext delegate;

        SimpleTestRootContext(Level level) {
            this.delegate = new ConsoleLoggingContext(level);
        }

        @Override
        public boolean isLogging(Level level) {
            return delegate.isLogging(level);
        }

        @Override
        public void log(Level level, String msg, Throwable e, Object... args) {
            delegate.log(level, msg, e, args);
        }

        @Override
        public void raw(Level level, String msg) {
            delegate.raw(level, msg);
        }

        @Override
        public void newline() {
            delegate.newline();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public void enableConsole(Level level) {
            // no-op for test context
        }

        @Override
        public String getProperty(String key, String defaultValue) {
            return defaultValue;
        }

        @Override
        public void shutdown() {
            // no-op
        }

        @Override
        public void enableFile(Level level, String logFile) {
            // no-op
        }

        @Override
        public void enableFile(Level level, java.io.File logFile) {
            // no-op
        }

        @Override
        public void enableFile(Level level, java.io.File logFile, int maxFiles, long maxSize) {
            // no-op
        }

        @Override
        public void reset() {
            // no-op
        }
    }
}
