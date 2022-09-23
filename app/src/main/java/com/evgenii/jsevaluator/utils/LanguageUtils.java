package com.evgenii.jsevaluator.utils;

import java.util.Locale;

/* loaded from: classes8.dex */
public class LanguageUtils {
    public static String getLanguage() {
        String locale = Locale.getDefault().toString();
        if (locale.contains("zh")) {
            return "zh";
        }
        return locale.contains("en") ? "en" : locale;
    }
}
