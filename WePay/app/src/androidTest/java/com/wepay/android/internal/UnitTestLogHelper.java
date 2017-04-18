package com.wepay.android.internal;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.wepay.android.WePay;
import com.wepay.android.enums.LogLevel;
import com.wepay.android.models.Config;

import junit.framework.Assert;

import org.junit.Test;

public class UnitTestLogHelper {
    private static final String CLIENT_ID = "171482";
    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();
    private static final String ENVIRONMENT = Config.ENVIRONMENT_STAGE;

    @Test
    public void testConfigWithLogLevel() {
        // Changing the config's log should not change the global log level until it is passed into
        // a WePay constructor.
        Config config = new Config(CONTEXT, CLIENT_ID, ENVIRONMENT);

        config.setLogLevel(LogLevel.NONE);
        Assert.assertNotSame(LogLevel.NONE, LogHelper.logLevel);

        WePay wePay = new WePay(config);
        Assert.assertEquals(config.getLogLevel(), LogHelper.logLevel);
    }


    @Test
    public void testMultipleConfigs() {
        // The SDK should respect the logLevel config belonging to the object most recently passed
        // to the WePay constructor.
        Config config0 = new Config(CONTEXT, CLIENT_ID, ENVIRONMENT);
        Config config1 = new Config(CONTEXT, CLIENT_ID, ENVIRONMENT);

        config0.setLogLevel(LogLevel.ALL);
        config1.setLogLevel(LogLevel.NONE);

        WePay wePay = new WePay(config0);
        wePay = new WePay(config1);

        Assert.assertEquals(config1.getLogLevel(), LogHelper.logLevel);
    }
}
