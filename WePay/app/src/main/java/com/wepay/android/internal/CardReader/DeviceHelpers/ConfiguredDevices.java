package com.wepay.android.internal.CardReader.DeviceHelpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class ConfiguredDevices {

    private final static String PREF_KEY_CONFIGURED_DEVICE_SIZE = "PREF_KEY_CONFIGURED_DEVICE_SIZE";
    private final static String PREF_KEY_DEVICE_CONFIG_HASH = "PREF_KEY_DEVICE_CONFIG_HASH_";

    public static Set<String> get(Context context) {
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

    public static boolean put(Context context, Set<String> deviceConfigHash) {
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

    private static boolean clear(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(PREF_KEY_CONFIGURED_DEVICE_SIZE);
        editor.remove(PREF_KEY_DEVICE_CONFIG_HASH);
        return editor.commit();
    }

}
