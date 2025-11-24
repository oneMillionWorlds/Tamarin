package com.onemillionworlds.tamarin.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SingleOccurrenceLogTest {

    private Logger logger;
    private CapturingHandler handler;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("test." + getClass().getName() + "." + System.nanoTime());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        handler = new CapturingHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
    }

    @AfterEach
    void tearDown() {
        logger.removeHandler(handler);
        handler.close();
    }

    @Test
    void identicalMessagesAreSuppressedWhenEnabled() {
        SingleOccurrenceLog sol = new SingleOccurrenceLog(logger);

        sol.info("Hello");
        sol.info("Hello"); // duplicate
        sol.warning("World");
        sol.warning("World"); // duplicate

        List<LogRecord> records = handler.getRecords();
        assertEquals(2, records.size(), "Only first occurrence of each unique message should be logged");
        assertEquals("Hello", records.get(0).getMessage());
        assertEquals(Level.INFO, records.get(0).getLevel());
        assertEquals("World", records.get(1).getMessage());
        assertEquals(Level.WARNING, records.get(1).getLevel());
    }

    @Test
    void suppressionCanBeDisabled() {
        SingleOccurrenceLog sol = new SingleOccurrenceLog(logger);
        sol.allowRepeatedLogs();
        sol.info("Repeat");
        sol.info("Repeat");
        sol.info("Repeat");

        List<LogRecord> records = handler.getRecords();
        assertEquals(3, records.size(), "When suppression is disabled, all messages should be logged");
        records.forEach(r -> assertEquals("Repeat", r.getMessage()));
    }

    @Test
    void clearSuppressedMessagesResetsState() {
        SingleOccurrenceLog sol = new SingleOccurrenceLog(logger);

        sol.severe("Once");
        sol.severe("Once"); // suppressed
        assertEquals(1, handler.getRecords().size());

        sol.clearSuppressedMessages();
        sol.severe("Once"); // should log again after clear

        List<LogRecord> records = handler.getRecords();
        assertEquals(2, records.size(), "After clearing, the same message should be logged again");
        assertEquals("Once", records.get(0).getMessage());
        assertEquals("Once", records.get(1).getMessage());
    }

    @Test
    void concurrentLoggingOfSameMessageOnlyLogsOnce() throws InterruptedException {
        SingleOccurrenceLog sol = new SingleOccurrenceLog(logger);

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    sol.log(Level.FINE, "SameMessage");
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "All tasks should complete promptly");
        pool.shutdownNow();

        List<LogRecord> records = handler.getRecords();
        assertEquals(1, records.size(), "Concurrent duplicate messages should be suppressed to a single log record");
        assertEquals("SameMessage", records.get(0).getMessage());
        assertEquals(Level.FINE, records.get(0).getLevel());
    }

    static class CapturingHandler extends Handler {
        private final List<LogRecord> records = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void publish(LogRecord record) {
            if (isLoggable(record)) {
                records.add(record);
            }
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() throws SecurityException {
            // no-op
        }

        List<LogRecord> getRecords() {
            return new ArrayList<>(records);
        }
    }
}
