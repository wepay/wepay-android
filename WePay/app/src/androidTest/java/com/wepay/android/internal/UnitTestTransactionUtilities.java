package com.wepay.android.internal;

import android.support.test.runner.AndroidJUnit4;

import com.wepay.android.internal.CardReader.Utilities.TransactionUtilities;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
// TODO: Rename to UnitTestTransactionUtilities
public class UnitTestTransactionUtilities {

    @Test
    public void testSanitizePAN() {
        TransactionUtilities transactionUtilities = new TransactionUtilities(null, null);
        String pan = "1111222233334444";
        String expected = "XXXXXXXXXXXX4444";

        String sanitized = transactionUtilities.sanitizePAN(pan);

        Assert.assertEquals(expected, sanitized);
    }
}
