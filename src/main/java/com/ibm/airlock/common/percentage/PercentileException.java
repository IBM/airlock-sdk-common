package com.ibm.airlock.common.percentage;

/**
 * Exception is thrown when an error happens during percentile calculation
 *
 * @author Denis Voloshin
 */
class PercentileException extends Exception {
    PercentileException(String message) {
        super(message);
    }
}
