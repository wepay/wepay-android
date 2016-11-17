package com.wepay.android.internal;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wepay.android.models.Config;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UnitTestWepayClient {
    private static final String CLIENT_ID = "171482";
    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();

    private static final String BASE_URL_STAGE = "https://stage.wepayapi.com/v2/";
    private static final String BASE_URL_PROD = "https://wepayapi.com/v2/";

    @Test
    public void testGetAbsoluteUrlStage() {
        Config config = new Config(CONTEXT, CLIENT_ID, Config.ENVIRONMENT_STAGE);
        String relativeUrl = "create_emv";
        String expected = BASE_URL_STAGE + relativeUrl;
        String result = WepayClient.getAbsoluteUrl(config, relativeUrl);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetAbsoluteUrlProduction() {
        Config config = new Config(CONTEXT, CLIENT_ID, Config.ENVIRONMENT_PRODUCTION);
        String relativeUrl = "create_emv";
        String expected = BASE_URL_PROD + relativeUrl;
        String result = WepayClient.getAbsoluteUrl(config, relativeUrl);

        Assert.assertEquals(expected, result);
    }
    @Test
    public void testGetAbsoluteUrlOtherEnvironment() {
        String environment = "https://wepay.com/";
        Config config = new Config(CONTEXT, CLIENT_ID, environment);
        String relativeUrl = "create_emv";
        String expected = environment + relativeUrl;
        String result = WepayClient.getAbsoluteUrl(config, relativeUrl);

        Assert.assertEquals(expected, result);
    }
}
