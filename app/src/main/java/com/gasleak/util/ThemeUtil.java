/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/traitimtrongvag/gasleakdetector-app
 * Modified: 2026-03-15
 */
package com.gasleak.util;

import android.app.Activity;
import com.gasleak.R;
import com.gasleak.data.prefs.SharedPrefs;

public class ThemeUtil {

    /**
     * Applies the theme saved in SharedPrefs.
     * Must be called before {@code super.onCreate()} and {@code setContentView()}.
     */
    public static void applyTheme(Activity activity) {
        applyTheme(activity, new SharedPrefs(activity).getTheme());
    }

    public static void applyTheme(Activity activity, int themeId) {
        activity.setTheme(themeId == 1 ? R.style.AppTheme_Dark : R.style.AppTheme);
    }
}
