package com.ibm.airlock.common;

/**
 * Used for the application to receive callbacks for sync functions.
 *
 * @author Denis Voloshin
 */
public interface AirlockCallback {
    /**
     * Is called when the function failed.
     *
     * @param e The exception.
     */
    void onFailure(Exception e);

    /**
     * Is called when the function returned successfully.
     *
     * @param msg response message. can be empty
     */
    void onSuccess(String msg);
}
