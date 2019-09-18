package com.ibm.airlock.common.util;

import java.util.Locale;

public class DefaultLocaleProvider implements LocaleProvider {

    private Locale locale;

    public DefaultLocaleProvider(String locale) {
        if (!locale.isEmpty()) {
            if (locale.split("_").length < 2) {
                this.locale = new Locale(locale);
            } else {
                this.locale = new Locale(locale.split("_")[0], locale.split("_")[1]);
            }
        }
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }
}
