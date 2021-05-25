package com.ibm.airlock.common.engine;

import com.ibm.airlock.common.util.Base64;


/**
 * maintain a random set of bits representing a percentage of users that are allowed to see a feature
 *
 * @author Denis Voloshin
 */
public class Percentile {
    private static final int maxNum = 100;
    private static final int byteSZ = Byte.SIZE;
    @SuppressWarnings("unused")
    static final int maxBytes = (maxNum / byteSZ) + ((maxNum % byteSZ > 0) ? 1 : 0);
    private final byte[] array;
    private int percentage;
    public Percentile(String b64) throws PercentileException {
        array = Base64.decode(b64);
        if (array.length != maxBytes) {
            throw new PercentileException("Invalid percentile string " + b64);
        }

        percentage = countOn();
    }

    private int countOn() {
        int count = 0;
        for (int i = 0; i < maxNum; ++i) {
            if (isOn(i)) {
                ++count;
            }
        }
        return count;
    }

    // given a user number from 0 to 99, see if it appears in the set of accepted numbers
    public boolean isAccepted(int userRandomNumber) throws PercentileException {
        // TODO just take (number % 100) - then any user number will do
        if (userRandomNumber < 0 || userRandomNumber >= maxNum) {
            throw new PercentileException("Invalid user random number " + userRandomNumber);
        }

        return isOn(userRandomNumber);
    }

    public int getPercentage() {
        return percentage;
    }

    private boolean isOn(int i) {
        int onBit = 1 << (i % byteSZ);
        return (array[i / byteSZ] & onBit) != 0;
    }

    public static class PercentileException extends Exception {
        private static final long serialVersionUID = 1L;

        PercentileException(String err) {
            super(err);
        }
    }
}

