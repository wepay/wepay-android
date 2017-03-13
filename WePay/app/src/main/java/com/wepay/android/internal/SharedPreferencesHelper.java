package com.wepay.android.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class SharedPreferencesHelper {

    private final static String PREF_KEY_CONFIGURED_DEVICE_SIZE = "PREF_KEY_CONFIGURED_DEVICE_SIZE";
    private final static String PREF_KEY_DEVICE_CONFIG_HASH = "PREF_KEY_DEVICE_CONFIG_HASH_";
    private final static String PREF_KEY_DEVICE_CALIBRATION = "PREF_KEY_DEVICE_CALIBRATION";
    private static final String PREF_KEY_CARDREADER_ID = "PREF_KEY_CARDREADER_ID";

    public static Set<String> getConfiguredDevices(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> deviceConfigHashes = new HashSet<String>();
        int size = sp.getInt(PREF_KEY_CONFIGURED_DEVICE_SIZE, 0);
        String serialNumber;
        for (int i = 0; i < size; i++) {
            serialNumber = sp.getString(PREF_KEY_DEVICE_CONFIG_HASH + i, null);
            if (!TextUtils.isEmpty(serialNumber)) {
                deviceConfigHashes.add(serialNumber);
            }
        }
        return deviceConfigHashes;
    }

    public static boolean saveConfiguredDevices(Context context, Set<String> deviceConfigHash) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_KEY_CONFIGURED_DEVICE_SIZE, deviceConfigHash.size());
        int i = 0;
        for (String str : deviceConfigHash) {
            editor.remove(PREF_KEY_DEVICE_CONFIG_HASH + i);
            editor.putString(PREF_KEY_DEVICE_CONFIG_HASH + i, str);
            i++;
        }
        return editor.commit();
    }

    public static boolean clearConfiguredDevices(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(PREF_KEY_CONFIGURED_DEVICE_SIZE);
        editor.remove(PREF_KEY_DEVICE_CONFIG_HASH);
        return editor.commit();
    }

    public static String getCalibration(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PREF_KEY_DEVICE_CALIBRATION, null);
    }

    public static boolean saveCalibration(Context context, String calibrationJSON) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_KEY_DEVICE_CALIBRATION, calibrationJSON);
        return editor.commit();
    }

    public static boolean clearCalibration(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(PREF_KEY_DEVICE_CALIBRATION);
        return editor.commit();
    }

    public static void rememberCardReader(String identifier, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(PREF_KEY_CARDREADER_ID, identifier);

        if (!editor.commit()) {
            Log.e("wepay_sdk", "Unable to write card reader with identifier " + identifier + " to disk.");
        }
    }

    public static String getRememberedCardReader(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String rememberedCardReader = preferences.getString(PREF_KEY_CARDREADER_ID, null);

        return rememberedCardReader;
    }

    public static void forgetRememberedCardReader(Context context) {
        SharedPreferencesHelper.rememberCardReader(null, context);
    }
}
