package com.ibm.airlock.common.log;

/**
 * Created by Denis Voloshin
 */

public final class Logger {
    public static Log log;

    private Logger() {
    }

    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static void setLogger(Log log) {
        Logger.log = log;
    }
}
