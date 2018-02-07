package com.wepay.android.internal;

import android.util.Log;

import com.wepay.android.enums.LogLevel;

/**
 * This is a wrapper class for all SDK logging. It allows for a single point of enabling/disabling
 * logs.
 */
public class LogHelper {
    public static LogLevel logLevel = LogLevel.ALL;

    private static final String TAG = "wepay_sdk";

    public static void log(String msg) {
        if (logLevel == LogLevel.ALL) {
            Log.i(TAG, msg);
        }
    }
}
