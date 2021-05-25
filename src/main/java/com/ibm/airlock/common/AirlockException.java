package com.ibm.airlock.common;

import java.io.IOException;


/**
 * This exception will be thrown
 *
 * @author Rachel Levy
 */
public class AirlockException extends IOException {
    /**
     * Construct a new AirlockException for general problems
     *
     * @param mag a message to be added to this exception
     */

    public AirlockException(String mag) {
        super(mag);
    }

    public AirlockException(String mag, int errorCode) {
        super(mag);
    }
}
