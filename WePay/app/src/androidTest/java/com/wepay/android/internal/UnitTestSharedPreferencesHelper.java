package com.wepay.android.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UnitTestSharedPreferencesHelper {
    private final Context CONTEXT = InstrumentationRegistry.getTargetContext();

    @After
    public void tearDown() {
        // Clear everything in the SharedPreferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CONTEXT);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    @Test
    public void testHappyPathRememberCardReader() {
        String cardReaderIdentifier = "Nimbus 2000";

        SharedPreferencesHelper.rememberCardReader(cardReaderIdentifier, CONTEXT);
        Assert.assertEquals(cardReaderIdentifier, SharedPreferencesHelper.getRememberedCardReader(CONTEXT));
    }

    @Test
    public void testHappyPathForgetCardReader() {
        String cardReaderIdentifier = "Nimbus 2000";
        String checkIdentifier;

        SharedPreferencesHelper.rememberCardReader(cardReaderIdentifier, CONTEXT);
        SharedPreferencesHelper.forgetRememberedCardReader(CONTEXT);
        checkIdentifier = SharedPreferencesHelper.getRememberedCardReader(CONTEXT);

        Assert.assertEquals(null, checkIdentifier);
    }

    @Test
    public void testGetRememberCardReaderWhenEmpty() {
        // Try to get the remembered card reader when nothing has been remembered yet.
        String emptyIdentifier = SharedPreferencesHelper.getRememberedCardReader(CONTEXT);

        Assert.assertEquals(null, emptyIdentifier);
    }
}
