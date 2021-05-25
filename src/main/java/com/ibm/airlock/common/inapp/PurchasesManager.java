package com.ibm.airlock.common.inapp;

import com.ibm.airlock.common.cache.PersistenceHandler;

/**
 * @author  Denis Voloshin
 */
public class PurchasesManager {
    private final String appVersion;
    private final PersistenceHandler ph;

    public PurchasesManager(PersistenceHandler ph, String appVersion) {
        this.ph = ph;
        this.appVersion = appVersion;
    }
}
