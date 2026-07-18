/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/gasleakdetector/gasleakdetector
 * Modified: 2026-06-19
 */
package com.gasleakdetector.util;

import android.app.Activity;
import com.gasleakdetector.R;
import com.gasleakdetector.data.prefs.SharedPrefs;

public class ThemeUtil {

    /* Theme IDs stored in SharedPrefs. */
    public static final int THEME_DEFAULT = 0;
    public static final int THEME_DARK    = 1;
    public static final int THEME_LIGHT   = 2;

    /**
     * Applies the theme saved in SharedPrefs.
     * Must be called before super.onCreate() and setContentView().
     */
    public static void applyTheme(Activity activity) {
        applyTheme(activity, new SharedPrefs(activity).getTheme());
    }

    public static void applyTheme(Activity activity, int themeId) {
        switch (themeId) {
            case THEME_LIGHT:
                activity.setTheme(R.style.AppTheme);
                break;
            case THEME_DARK:
                activity.setTheme(R.style.AppTheme_Dark);
                break;
            case THEME_DEFAULT:
            default:
                activity.setTheme(R.style.AppTheme_Default);
                break;
        }
    }
}
