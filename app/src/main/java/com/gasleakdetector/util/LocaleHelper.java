/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-04-15
 */
package com.gasleakdetector.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

/** Applies a language preference to an Activity or Application context. */
public class LocaleHelper {

    private static Locale getLocaleForCode(String langCode) {
        if (langCode == null || langCode.isEmpty()) return Locale.ENGLISH;
        switch (langCode) {
            case "zh": return Locale.SIMPLIFIED_CHINESE;
            case "ja": return Locale.JAPAN;
            case "ko": return new Locale("ko", "KR");
            case "fr": return Locale.FRANCE;
            case "es": return new Locale("es", "ES");
            case "de": return Locale.GERMANY;
            case "vi": return new Locale("vi", "VN");
            default:   return Locale.ENGLISH;
        }
    }

    public static Context applyLocale(Context context, String langCode) {
        Locale locale = getLocaleForCode(langCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }
}
