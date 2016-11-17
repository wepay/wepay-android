package com.wepay.android.models;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UnitTestPaymentToken {
    private String testToken;
    private PaymentToken paymentToken;

    @Before
    public void setUp() {
        testToken = "1234567890";
        paymentToken = new PaymentToken(testToken);
    }

    @Test
    public void testToString() {
        String expected = "{\"tokenId\":\"" + this.testToken + "\"}";

        String result = paymentToken.toString();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetToken() {
        String result = paymentToken.getTokenId();

        Assert.assertEquals(testToken, result);
    }
}
