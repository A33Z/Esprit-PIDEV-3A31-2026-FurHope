package com.esprit.animal.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

public final class LanguageManager {

    private static final String BUNDLE_BASE_NAME = "animal.i18n.messages";
    private static Locale currentLocale = Locale.FRENCH;

    private LanguageManager() {
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void setLocale(Locale locale) {
        if (locale == null) {
            return;
        }
        currentLocale = locale;
    }

    public static void setLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return;
        }
        currentLocale = Locale.forLanguageTag(languageCode);
    }

    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
    }

    public static String get(String key) {
        return getBundle().getString(key);
    }
}

