package com.ibm.airlock.common;

/**
 * This exception will be thrown
 *
 * @author Rachel Levy
 */
public class AirlockInvalidFileException extends Exception {
    /**
     * Construct a new AirlockInvalidFileException
     *
     * @param mag a message to be added to this exception
     */
    public AirlockInvalidFileException(String mag) {
        super(mag);
    }
}
