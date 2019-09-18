package com.ibm.airlock.common.services;

import com.ibm.airlock.common.dependency.ProductDiComponent;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.LocaleProvider;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class StringsService {

    private boolean isDoubleLengthStrings = false;

    @Inject
    InfraAirlockService infraAirlockService;

    public void init(ProductDiComponent productDiComponent) {
        productDiComponent.inject(this);
    }

    /**
     * @return whether AirlockManager.getString methods should return the same string as a doubled value
     */
    public boolean isDoubleLengthStrings() {
        return isDoubleLengthStrings;
    }

    /**
     * Sets the whether SDK should be configured to return AirlockManager.getString as as a doubled value
     *
     * @param doubleLengthStrings true if should return as as a doubled value
     */
    public void setDoubleLengthStrings(boolean doubleLengthStrings) {
        isDoubleLengthStrings = doubleLengthStrings;
    }

    /**
     * Returns a translated UI string based on the current device locale. When the key (string ID) is not found, returns null.
     *
     * @param key  represents a string value
     * @param args a list of string arguments if it contains placeholders
     * @return translated UI string based on the current device locale
     */
    @CheckForNull
    public String getString(String key, String... args) {
        JSONObject translations = infraAirlockService.getPersistenceHandler().readJSON(Constants.SP_RAW_TRANSLATIONS);
        if (translations == null || translations.optJSONObject("strings") == null) {
            return null;
        }
        JSONObject translationsTable = translations.optJSONObject("strings");
        String translatedValue = translationsTable.optString(key);
        if (translatedValue == null) {
            return null;
        }

        for (int i = 0; i < args.length; i++) {
            translatedValue = translatedValue.replace("[[[" + (i + 1) + "]]]", args[i]);
        }

        return (this.isDoubleLengthStrings) ? translatedValue + ' ' + translatedValue : translatedValue;
    }

    public Map<String, String> getAllStrings() {
        Map<String, String> allStrings = new Hashtable<>();
        JSONObject translations = infraAirlockService.getPersistenceHandler().readJSON(Constants.SP_RAW_TRANSLATIONS);
        if (translations == null || translations.optJSONObject("strings") == null) {
            return allStrings;
        }
        JSONObject translationsTable = translations.optJSONObject("strings");
        if (translationsTable == null) {
            return allStrings;
        }

        Iterator<String> keys = translationsTable.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = translationsTable.optString(key);
            if (value != null) {
                allStrings.put(key, value);
            }
        }
        return allStrings;
    }

    /**
     * Check if the device locale has been changed.
     * If it changed, clear all available runtime model and return the
     * application to the default state.
     */
    public void resetLocale() {
        infraAirlockService.resetLocale();
    }

    public void setLocaleProvider(LocaleProvider localeProvider) {
        infraAirlockService.setLocaleProvider(localeProvider);
        infraAirlockService.getPersistenceHandler().write(Constants.SP_CURRENT_LOCALE, localeProvider.getLocale().toString());
    }

    public Locale getLocale() {
        return infraAirlockService.getLocale();
    }
}
