package com.esprit.utils;

import java.io.InputStream;
import java.util.Properties;

public final class PhoneUtils {

    private static final String DEFAULT_COUNTRY_CODE = resolveDefaultCountryCode();

    private PhoneUtils() {
    }

    public static String normalizeForSms(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }

        String phone = rawPhone.trim().replace(" ", "").replace("-", "");
        if (phone.startsWith("00")) {
            phone = "+" + phone.substring(2);
        }

        if (phone.startsWith("+")) {
            return "+" + phone.substring(1).replaceAll("\\D", "");
        }

        String digits = phone.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "";
        }

        String countryDigits = DEFAULT_COUNTRY_CODE.replaceAll("\\D", "");
        if (!countryDigits.isEmpty() && digits.startsWith(countryDigits) && digits.length() > countryDigits.length() + 6) {
            return "+" + digits;
        }

        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        return DEFAULT_COUNTRY_CODE + digits;
    }

    public static boolean isLikelyE164(String phone) {
        return phone != null && phone.matches("^\\+[1-9][0-9]{7,14}$");
    }

    private static String resolveDefaultCountryCode() {
        String fromEnv = System.getenv("APP_DEFAULT_COUNTRY_CODE");
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv.trim();
        }
        try (InputStream input = PhoneUtils.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties properties = new Properties();
                properties.load(input);
                String fromProperties = properties.getProperty("app.default_country_code");
                if (fromProperties != null && !fromProperties.trim().isEmpty()) {
                    return fromProperties.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "+216";
    }
}
