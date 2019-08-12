package com.ibm.airlock.common.exceptions;

import java.io.IOException;

/**
 * General Airlock Exception
 *
 * @author Denis Voloshin
 */
public class AirlockException extends IOException {
    private static final long serialVersionUID = -4362013323810514072L;

    /**
     * Construct a new AirlockException for general problems
     *
     * @param massage a message to be added to this exception
     */
    public AirlockException(String massage) {
        super(massage);
    }
}
