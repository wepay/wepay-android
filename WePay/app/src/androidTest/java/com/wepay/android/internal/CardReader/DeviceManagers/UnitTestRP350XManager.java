package com.wepay.android.internal.CardReader.DeviceManagers;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wepay.android.enums.ErrorCode;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UnitTestRP350XManager {
    private static final String CLIENT_ID = "171482";
    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();
    private static final String ENVIRONMENT = Config.ENVIRONMENT_STAGE;
    private Config config = new Config(CONTEXT, CLIENT_ID, ENVIRONMENT);

    @Test
    public void testShouldKeepWaitingForCard1() {
        // Constructor of RP350XManager calls `new Handler()`
        // which requires the current thread to be a looper thread
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        RP350XManager rp350XManager = new RP350XManager(config, null, null, null);
        Integer errorCode = ErrorCode.CARD_READER_GENERAL_ERROR.getCode();
        Error error = new Error(errorCode, Error.ERROR_DOMAIN_SDK, null, null);

        boolean result = rp350XManager.shouldRestartTransaction(error, PaymentMethod.SWIPE);
        Assert.assertEquals(true, result);
    }

    @Test
    public void testShouldKeepWaitingForCard2() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        RP350XManager rp350XManager = new RP350XManager(config, null, null, null);
        Integer errorCode = ErrorCode.CARD_READER_GENERAL_ERROR.getCode();
        Error error = new Error(errorCode, Error.ERROR_DOMAIN_API, null, null);

        boolean result = rp350XManager.shouldRestartTransaction(error, PaymentMethod.SWIPE);
        Assert.assertEquals(false, result);
    }

    @Test
    public void testShouldKeepWaitingForCard3() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        RP350XManager rp350XManager = new RP350XManager(config, null, null, null);
        Error error = new Error(null, Error.ERROR_DOMAIN_API, null, null);

        boolean result = rp350XManager.shouldRestartTransaction(error, PaymentMethod.SWIPE);
        Assert.assertEquals(false, result);
    }
}
