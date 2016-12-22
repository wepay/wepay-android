package com.wepay.android.internal.CardReader.DeviceHelpers;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.roam.roamreaderunifiedapi.constants.ResponseCode;
import com.wepay.android.enums.ErrorCode;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class UnitTestDipTransactionHelper {
    private static final String CLIENT_ID = "171482";
    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();
    private static final String ENVIRONMENT = Config.ENVIRONMENT_STAGE;
    private Config config = new Config(CONTEXT, CLIENT_ID, ENVIRONMENT);
    private DipTransactionHelper dipTransactionHelper = new DipTransactionHelper(config, null, null, null);

    @Test
    public void testShouldReactToError() {
        Integer errorCode = ErrorCode.EMV_TRANSACTION_ERROR.getCode();
        String errorDescription = com.roam.roamreaderunifiedapi.constants.ErrorCode.CardReaderNotConnected.toString();
        Error error = new Error(errorCode, null, null, errorDescription);

        boolean actual = dipTransactionHelper.shouldReactToError(error);

        Assert.assertEquals(false, actual);
    }

    @Test
    public void testCreateAuthInfo() {
        String tc = "0123456789ABCDEF";
        String expectedToken = "MDEyMzQ1Njc4OUFCQ0RFRitudWxs";

        String actualToken = dipTransactionHelper.createAuthInfo(tc).getTransactionToken();

        Assert.assertEquals(expectedToken, actualToken);
    }

    @Test
    public void testShouldExecuteMagicNumbers1() {
        dipTransactionHelper.amount = new BigDecimal("21.61");
        boolean result = dipTransactionHelper.shouldExecuteMagicNumbers();

        Assert.assertEquals(true, result);
    }

    @Test
    public void testShouldExecuteMagicNumbers2() {
        dipTransactionHelper.amount = new BigDecimal("10");
        boolean result = dipTransactionHelper.shouldExecuteMagicNumbers();

        Assert.assertEquals(false, result);
    }

    @Test
    public void testValidateEMVResponse_UnknownError() {
        Map<Parameter, Object> data = new HashMap<>();
        data.put(Parameter.ResponseCode, ResponseCode.Error);
        Error expected = Error.getEmvTransactionErrorWithMessage("unknown");

        Error result = dipTransactionHelper.validateEMVResponse(data);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testValidateEMVResponse_RSAKeyNotFound() {
        Map<Parameter, Object> data = new HashMap<>();
        data.put(Parameter.ResponseCode, ResponseCode.Error);
        data.put(Parameter.ErrorCode, com.roam.roamreaderunifiedapi.constants.ErrorCode.RSAKeyNotFound);
        Error expected = Error.getCardNotSupportedError();

        Error result = dipTransactionHelper.validateEMVResponse(data);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testValidateEMVResponse_CardExpired() {
        Map<Parameter, Object> data = new HashMap<>();
        data.put(Parameter.ResponseCode, ResponseCode.Error);
        data.put(Parameter.ErrorCode, com.roam.roamreaderunifiedapi.constants.ErrorCode.CardExpired);
        Error expected = Error.getCardReaderGeneralErrorWithMessage("Card has expired");

        Error result = dipTransactionHelper.validateEMVResponse(data);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testValidateEMVResponse_ApplicationBlocked() {
        Map<Parameter, Object> data = new HashMap<>();
        data.put(Parameter.ResponseCode, ResponseCode.Error);
        data.put(Parameter.ErrorCode, com.roam.roamreaderunifiedapi.constants.ErrorCode.ApplicationBlocked);
        Error expected = Error.getCardBlockedError();

        Error result = dipTransactionHelper.validateEMVResponse(data);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testValidateEMVResponse_TimeoutExpired() {
        Map<Parameter, Object> data = new HashMap<>();
        data.put(Parameter.ResponseCode, ResponseCode.Error);
        data.put(Parameter.ErrorCode, com.roam.roamreaderunifiedapi.constants.ErrorCode.TimeoutExpired);
        Error expected = Error.getCardReaderTimeoutError();

        Error result = dipTransactionHelper.validateEMVResponse(data);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testConvertToEMVAmount() {
        String expected = "000000002161";
        String result = dipTransactionHelper.convertToEMVAmount(new BigDecimal("21.61"));

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testConvertToEmvCurrencyCode() {
        String expected = "0840";
        String result = dipTransactionHelper.convertToEmvCurrencyCode("USD");

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testConvertResponseCodeToHexString() {
        String expected = "726573706f6e7365";
        String result = dipTransactionHelper.convertResponseCodeToHexString("response");

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testShouldKeepWaitingForCard1() {
        // Constructor of IngenicoCardReaderManager calls `new Handler()`
        // which requires the current thread to be a looper thread
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        Integer errorCode = ErrorCode.CARD_READER_GENERAL_ERROR.getCode();
        Error error = new Error(errorCode, Error.ERROR_DOMAIN_SDK, null, null);

        boolean result = dipTransactionHelper.shouldRestartTransaction(error, PaymentMethod.SWIPE);
        Assert.assertEquals(true, result);
    }

    @Test
    public void testShouldKeepWaitingForCard2() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        Integer errorCode = ErrorCode.CARD_READER_GENERAL_ERROR.getCode();
        Error error = new Error(errorCode, Error.ERROR_DOMAIN_API, null, null);

        boolean result = dipTransactionHelper.shouldRestartTransaction(error, PaymentMethod.SWIPE);
        Assert.assertEquals(false, result);
    }

    @Test
    public void testShouldKeepWaitingForCard3() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        Error error = new Error(null, Error.ERROR_DOMAIN_API, null, null);

        boolean result = dipTransactionHelper.shouldRestartTransaction(error, PaymentMethod.SWIPE);
        Assert.assertEquals(false, result);
    }
}
