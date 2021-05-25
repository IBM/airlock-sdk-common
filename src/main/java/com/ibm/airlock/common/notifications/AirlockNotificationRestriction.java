package com.ibm.airlock.common.notifications;

/**
 * Created by SEitan on 04/12/2017.
 */

class AirlockNotificationRestriction {

    int maxAllowed;
    int minInterval;
    String name;

    public AirlockNotificationRestriction(String name, int maxAllowed, int minInterval) {
        this.maxAllowed = maxAllowed;
        this.minInterval = minInterval;
        this.name = name;
    }

    public int getMaxAllowed() {
        return maxAllowed;
    }

    public int getMinInterval() {
        return minInterval;
    }

    public String getName() {
        return name;
    }
}
