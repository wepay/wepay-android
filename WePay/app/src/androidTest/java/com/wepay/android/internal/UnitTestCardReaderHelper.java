package com.wepay.android.internal;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UnitTestCardReaderHelper {

    @Test
    public void testSanitizePAN() {
        CardReaderHelper cardReaderHelper = new CardReaderHelper(null);
        String pan = "1111222233334444";
        String expected = "XXXXXXXXXXXX4444";

        String sanitized = cardReaderHelper.sanitizePAN(pan);

        Assert.assertEquals(expected, sanitized);
    }
}
