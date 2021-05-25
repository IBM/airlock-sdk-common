package com.ibm.airlock.common;

/**
 * This exception will be thrown
 *
 * @author Rachel Levy
 */
public class AirlockMismatchSeasonException extends Exception {
    /**
     * Construct a new AirlockMismatchSeasonException
     *
     * @param mag a message to be added to this exception
     */
    public AirlockMismatchSeasonException(String mag) {
        super(mag);
    }
}
