package com.onemillionworlds.tamarin.logging;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to suppress multiple identical instances of the same log
 */
public class SingleOccurrenceLog {
    public static boolean SUPPRESS_REPEATED_LOGS = true;

    private final Logger baseLogger;

    private final Set<String> messagesAlreadyLogged = ConcurrentHashMap.newKeySet();

    public SingleOccurrenceLog(Logger baseLogger) {
        this.baseLogger = baseLogger;
    }

    private boolean shouldLog(String message) {
        if (!SUPPRESS_REPEATED_LOGS) {
            return true;
        }
        // add() returns true if this set did not already contain the specified element
        return messagesAlreadyLogged.add(message);
    }

    public void log(Level level, String msg) {
        if (shouldLog(msg)) {
            baseLogger.log(level, msg);
        }
    }

    public void log(Level level, String msg, Throwable thrown) {
        if (shouldLog(msg)) {
            baseLogger.log(level, msg, thrown);
        }
    }

    public void info(String msg) {
        if (shouldLog(msg)) {
            baseLogger.info(msg);
        }
    }

    public void warning(String msg) {
        if (shouldLog(msg)) {
            baseLogger.warning(msg);
        }
    }

    public void severe(String msg) {
        if (shouldLog(msg)) {
            baseLogger.severe(msg);
        }
    }

    public void fine(String msg) {
        if (shouldLog(msg)) {
            baseLogger.fine(msg);
        }
    }

    public void finer(String msg) {
        if (shouldLog(msg)) {
            baseLogger.finer(msg);
        }
    }

    public void finest(String msg) {
        if (shouldLog(msg)) {
            baseLogger.finest(msg);
        }
    }

    /**
     * Clears the record of already-logged messages. Useful for testing or when a new phase starts
     * and duplicate suppression should reset.
     */
    public void clearSuppressedMessages() {
        messagesAlreadyLogged.clear();
    }
}
