package com.ibm.airlock.common.log;

/**
 * Created by Denis Voloshin on 01/11/2017.
 */

public interface Log {
    /**
     * Send an error log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public int e(String tag, String msg);

    /**
     * Send a error log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public int e(String tag, String msg, Throwable tr);

    /**
     * Send an warning log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public int w(String tag, String msg);

    /**
     * Send an debug log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public int d(String tag, String msg);

    /**
     * Send an info log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public int i(String tag, String msg);
}
