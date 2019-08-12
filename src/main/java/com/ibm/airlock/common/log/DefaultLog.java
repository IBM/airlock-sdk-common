package com.ibm.airlock.common.log;


/**
 * Created by Denis Voloshin
 *
 * Empty {@link Log} implementation
 */

public class DefaultLog implements Log {

    @Override
    public int e(String tag, String msg) {
        return 1;
    }

    @Override
    public int e(String tag, String msg, Throwable tr) {
        return 1;
    }

    @Override
    public int w(String tag, String msg) {
        return 1;
    }

    @Override
    public int d(String tag, String msg) {
        return 1;
    }

    @Override
    public int i(String tag, String msg) {
        return 1;
    }
}
