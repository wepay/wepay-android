package com.wepay.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wepay.android.enums.CalibrationResult;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.CurrencyCode;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.models.AuthorizationInfo;
import com.wepay.android.models.CalibrationParameters;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.MockConfig;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class IntegrationTest {
    private static final String CLIENT_ID = "171482";
    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();
    private static final String ENVIRONMENT = Config.ENVIRONMENT_STAGE;
    private Handler handler = new Handler(Looper.getMainLooper());

    private void runTestOnUiThread(Runnable r) {
        handler.post(r);
    }

    private Config getConfig() {
        return new Config(CONTEXT, CLIENT_ID, ENVIRONMENT).setUseLocation(true).setMockConfig(new MockConfig()
                .setUseMockCardReader(true)
                .setUseMockWepayClient(true));
    }

    private Address getAddress() {
        Address address = new Address(Locale.getDefault());
        address.setAddressLine(0, "380 Portage ave");
        address.setLocality("Palo Alto");
        address.setPostalCode("94306");
        address.setCountryCode("US");
        return address;
    }

    /**
     * Integration tests for tokenization without card reading
     */
    @Test
    public void testValidTokenization() throws InterruptedException {
        Config config = getConfig();
        final WePay wePay = new WePay(config);

        Address address = getAddress();
        final PaymentInfo paymentInfo = new PaymentInfo("Android", "Tester", "a@b.com",
                "Visa xxxx-1234", address, address, PaymentMethod.MANUAL, "4242424242424242", "123", "01", "25", true);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(countDownLatch) {
            @Override
            public void onSuccess(PaymentInfo paymentInfo, PaymentToken token) {
                super.onSuccess(paymentInfo, token);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.tokenize(paymentInfo, tokenizationHandler);
            }
        });

        countDownLatch.await(3000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(tokenizationHandler.onSuccessCalled);
        Assert.assertNotNull(tokenizationHandler.paymentToken);
    }

    @Test
    public void testInvalidTokenization() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setCardTokenizationFailure(true);
        final WePay wePay = new WePay(config);

        Address address = getAddress();
        final PaymentInfo paymentInfo = new PaymentInfo("Android", "Tester", "a@b.com",
                "Visa xxxx-1234", address, address, PaymentMethod.MANUAL, "424242442424242", "123", "01", "25", true);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(countDownLatch) {
            @Override
            public void onError(PaymentInfo paymentInfo, Error error) {
                super.onError(paymentInfo, error);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.tokenize(paymentInfo, tokenizationHandler);
            }
        });

        countDownLatch.await(3000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(tokenizationHandler.onErrorCalled);
        Assert.assertNotNull(tokenizationHandler.error);
    }

    /**
     * Integration tests for card reader's reading function
     */
    @Test
    public void testReaderConnectionTimeout() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setCardReadTimeout(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onStatusChange(CardReaderStatus status) {
                if (status == CardReaderStatus.NOT_CONNECTED) {
                    notConnectedStatusChangeCalled = true;
                    countDownLatch.countDown();
                }
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForReading(cardReaderHandler);
            }
        });

        // Waiting for RP350X_CONNECTION_TIME_SEC = 7, set in RP350XManager
        countDownLatch.await(7500, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.notConnectedStatusChangeCalled);
    }

    @Test
    public void testCardReadSuccess() throws InterruptedException {
        final Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onSuccess(PaymentInfo paymentInfo) {
                super.onSuccess(paymentInfo);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForReading(cardReaderHandler);
            }
        });

        // Waiting for READER_CONNECTION_TIME_MS = 200, set in MockRoamDeviceManager
        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onSuccessCalled);
        Assert.assertNotNull(cardReaderHandler.paymentInfo);
    }

    @Test
    public void testCardReadError() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setCardReadFailure(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onError(Error error) {
                super.onError(error);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForReading(cardReaderHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertEquals(Error.getEmvTransactionErrorWithMessage("UnknownError"), cardReaderHandler.error);
    }

    @Test
    public void testEmailInsertion() throws InterruptedException {
        Config config = getConfig();

        final String EMAIL = "a@b.com";
        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onPayerEmailRequested(CardReaderEmailCallback callback) {
                callback.insertPayerEmail(EMAIL);
            }

            @Override
            public void onSuccess(PaymentInfo paymentInfo) {
                super.onSuccess(paymentInfo);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForReading(cardReaderHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertEquals(EMAIL, cardReaderHandler.paymentInfo.getEmail());
    }

    @Test
    public void testReaderResetRequest() throws InterruptedException {
        Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onReaderResetRequested(CardReaderResetCallback callback) {
                onReaderResetRequestedCalled = true;
                callback.resetCardReader(true);
            }

            @Override
            public void onStatusChange(CardReaderStatus status) {
                if (status == CardReaderStatus.CONFIGURING_READER) {
                    onConfiguringReaderStatusChangeCalled = true;
                    countDownLatch.countDown();
                }
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForReading(cardReaderHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onReaderResetRequestedCalled);
        Assert.assertTrue(cardReaderHandler.onConfiguringReaderStatusChangeCalled);
    }

    /**
     * Integration tests for card reader's tokenization functionality (Swipe).
     */
    @Test
    public void testSwipeTokenizeSuccess() throws InterruptedException {
        Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(null);
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(countDownLatch) {
            @Override
            public void onSuccess(PaymentInfo paymentInfo, PaymentToken token) {
                super.onSuccess(paymentInfo, token);
                countDownLatch.countDown();
            }
        };
        final TestAuthorizationHandler authorizationHandler = new TestAuthorizationHandler(null);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(3000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(tokenizationHandler.onSuccessCalled);
        Assert.assertNotNull(tokenizationHandler.paymentToken);
    }

    @Test
    public void testSwipeTokenizeError() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setCardTokenizationFailure(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(null);
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(countDownLatch) {
            @Override
            public void onError(PaymentInfo paymentInfo, Error error) {
                super.onError(paymentInfo, error);
                countDownLatch.countDown();
            }
        };
        final TestAuthorizationHandler authorizationHandler = new TestAuthorizationHandler(null);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(3000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(tokenizationHandler.onErrorCalled);
        Assert.assertNotNull(tokenizationHandler.error);
    }

    /**
     * Integration tests for card reader's authorization functionality (EMV).
     */
    @Test
    public void testEMVAuthorizeSuccess() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(null);
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(null);
        final TestAuthorizationHandler authorizationHandler = new TestAuthorizationHandler(countDownLatch) {
            @Override
            public void onAuthorizationSuccess(PaymentInfo paymentInfo, AuthorizationInfo authorizationInfo) {
                super.onAuthorizationSuccess(paymentInfo, authorizationInfo);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(3000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(authorizationHandler.onAuthorizationSuccessCalled);
        Assert.assertNotNull(authorizationHandler.authorizationInfo);
    }

    @Test
    public void testEMVAuthorizeError() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP).setEMVAuthFailure(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(null) {
            @Override
            public void onTransactionInfoRequested(CardReaderTransactionInfoCallback callback) {
                // 20.61 is the magic number that will lead to authorization error.
                callback.useTransactionInfo(new BigDecimal("20.61"), CurrencyCode.USD, 1170640190);
            }
        };
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(null);
        final TestAuthorizationHandler authorizationHandler = new TestAuthorizationHandler(countDownLatch) {
            @Override
            public void onAuthorizationError(PaymentInfo paymentInfo, Error error) {
                super.onAuthorizationError(paymentInfo, error);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(4000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(authorizationHandler.onAuthorizationErrorCalled);
    }

    @Test
    public void testEMVApplicationSelectionSuccess() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP).setMultipleEMVApplication(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(null);
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(null);
        final TestAuthorizationHandler authorizationHandler = new TestAuthorizationHandler(countDownLatch) {
            @Override
            public void onAuthorizationSuccess(PaymentInfo paymentInfo, AuthorizationInfo authorizationInfo) {
                super.onAuthorizationSuccess(paymentInfo, authorizationInfo);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(authorizationHandler.onEMVApplicationSelectionRequestedCalled);

        // Asserting the right application (last one among four to choose from) was selected
        String paymentDescription = authorizationHandler.paymentInfo == null ? null : authorizationHandler.paymentInfo.getPaymentDescription();
        String lastFourDigitsOfPAN = paymentDescription == null ? null : paymentDescription.substring(paymentDescription.length() - 4);
        Assert.assertEquals("4444", lastFourDigitsOfPAN);
    }

    @Test
    public void testEMVApplicationSelectionError() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP).setMultipleEMVApplication(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onError(Error error) {
                super.onError(error);
                countDownLatch.countDown();
            }
        };
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(null);
        final AuthorizationHandler authorizationHandler = new TestAuthorizationHandler(null) {
            @Override
            public void onEMVApplicationSelectionRequested(ApplicationSelectionCallback callback, ArrayList<String> applications) {
                callback.useApplicationAtIndex(-1);
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidApplicationIdError(), cardReaderHandler.error);
    }

    @Test
    public void testTransactionInfoSuccess() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(null);
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(null);
        final TestAuthorizationHandler authorizationHandler = new TestAuthorizationHandler(countDownLatch) {
            @Override
            public void onAuthorizationSuccess(PaymentInfo paymentInfo, AuthorizationInfo authorizationInfo) {
                super.onAuthorizationSuccess(paymentInfo, authorizationInfo);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        // 24.61 is the magic number that will lead to authorization success, used in TestCardReaderHandler.
        Assert.assertEquals(new BigDecimal("24.61"), authorizationHandler.authorizationInfo.getAuthorizedAmount());
    }

    // Possible errors returned by validateAuthInfo() method in RP350XManager class
    @Test
    public void testTransactionInfoErrorForAmountTooSmall() throws InterruptedException {
        Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onTransactionInfoRequested(CardReaderTransactionInfoCallback callback) {
                callback.useTransactionInfo(new BigDecimal("0.90"), CurrencyCode.USD, 1170640190);
            }

            @Override
            public void onError(Error error) {
                super.onError(error);
                countDownLatch.countDown();
            }
        };
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(null);
        final TestAuthorizationHandler authorizationHandler = new TestAuthorizationHandler(null);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidTransactionInfoError(), cardReaderHandler.error);
    }

    @Test
    public void testTransactionInfoErrorForAmountNull() throws InterruptedException {
        Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onTransactionInfoRequested(CardReaderTransactionInfoCallback callback) {
                callback.useTransactionInfo(null, CurrencyCode.USD, 1170640190);
            }

            @Override
            public void onError(Error error) {
                super.onError(error);
                countDownLatch.countDown();
            }
        };
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(null);
        final TestAuthorizationHandler authorizationHandler = new TestAuthorizationHandler(null);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidTransactionInfoError(), cardReaderHandler.error);
    }

    @Test
    public void testTransactionInfoErrorForAmountTooPrecise() throws InterruptedException {
        Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onTransactionInfoRequested(CardReaderTransactionInfoCallback callback) {
                callback.useTransactionInfo(new BigDecimal("0.999"), CurrencyCode.USD, 1170640190);
            }

            @Override
            public void onError(Error error) {
                super.onError(error);
                countDownLatch.countDown();
            }
        };
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(null);
        final TestAuthorizationHandler authorizationHandler = new TestAuthorizationHandler(null);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidTransactionInfoError(), cardReaderHandler.error);
    }

    @Test
    public void testTransactionInfoErrorForInvalidAccountId() throws InterruptedException {
        Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onTransactionInfoRequested(CardReaderTransactionInfoCallback callback) {
                callback.useTransactionInfo(new BigDecimal("10.00"), CurrencyCode.USD, 0);
            }

            @Override
            public void onError(Error error) {
                super.onError(error);
                countDownLatch.countDown();
            }
        };
        final TestTokenizationHandler tokenizationHandler = new TestTokenizationHandler(null);
        final TestAuthorizationHandler authorizationHandler = new TestAuthorizationHandler(null);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidTransactionInfoError(), cardReaderHandler.error);
    }

    @Test
    public void testStopCardReader() throws InterruptedException {
        Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onTransactionInfoRequested(CardReaderTransactionInfoCallback callback) {
                // This callback is overridden with empty body so startCardReaderForReading()
                // is stuck here and not calling stopDevice()
            }

            @Override
            public void onStatusChange(CardReaderStatus status) {
                if (status == CardReaderStatus.STOPPED) {
                    onStoppedCalled = true;
                    countDownLatch.countDown();
                }
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForReading(cardReaderHandler);
                // Calling startCardReaderForReading() is needed to pass in the CardReaderHandler instance
                // so the onStatusChange() callback can be called with `STOPPED` status
                wePay.stopCardReader();
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onStoppedCalled);
    }

    @Test
    public void testCalibrateCardReader() throws InterruptedException {
        Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCalibrationHandler calibrationHandler = new TestCalibrationHandler(countDownLatch) {
            @Override
            public void onComplete(CalibrationResult result, CalibrationParameters params) {
                super.onComplete(result, params);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.calibrateCardReader(calibrationHandler);
            }
        });

        countDownLatch.await(50, TimeUnit.MILLISECONDS);

        Assert.assertTrue(calibrationHandler.onCompleteCalled);
    }

    @Test
    public void testBatteryInfoSuccess() throws InterruptedException {
        Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestBatteryLevelHandler batteryLevelHandler = new TestBatteryLevelHandler(countDownLatch) {
            @Override
            public void onBatteryLevel(int batteryLevel) {
                super.onBatteryLevel(batteryLevel);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.getCardReaderBatteryLevel(batteryLevelHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(batteryLevelHandler.onBatteryLevelCalled);
    }

    @Test
    public void testBatteryInfoError() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setBatteryLevelError(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestBatteryLevelHandler batteryLevelHandler = new TestBatteryLevelHandler(countDownLatch);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.getCardReaderBatteryLevel(batteryLevelHandler);
            }
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(batteryLevelHandler.onBatteryLevelErrorCalled);
    }

    /**
     * Integration tests for signature storing.
     */
    @Test
    public void testStoreSignatureSuccess() throws InterruptedException{
        Config config = getConfig();
        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCheckoutHandler checkoutHandler = new TestCheckoutHandler(countDownLatch) {
            @Override
            public void onSuccess(String signatureUrl, String checkoutId) {
                super.onSuccess(signatureUrl, checkoutId);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.storeSignatureImage(Bitmap.createBitmap(128, 128, Bitmap.Config.ALPHA_8), "checkout id", checkoutHandler);
            }
        });

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);

        // This test can only pass when using mocked WepayClient implementation
        if (config.getMockConfig().isUseMockWepayClient()) {
            Assert.assertTrue(checkoutHandler.onSuccessCalled);
        }
    }

    @Test
    public void testStoreSignatureInvalidImgError() throws InterruptedException{
        Config config = getConfig();
        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCheckoutHandler checkoutHandler = new TestCheckoutHandler(countDownLatch) {
            @Override
            public void onError(Bitmap image, String checkoutId, Error error) {
                super.onError(image, checkoutId, error);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // image is null
                wePay.storeSignatureImage(null, "checkout id", checkoutHandler);
            }
        });

        countDownLatch.await(50, TimeUnit.MILLISECONDS);

        Assert.assertTrue(checkoutHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidSignatureImageError(null), checkoutHandler.error);
    }

    @Test
    public void testStoreSignatureInvalidImgError1() throws InterruptedException{
        Config config = getConfig();
        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCheckoutHandler checkoutHandler = new TestCheckoutHandler(countDownLatch) {
            @Override
            public void onError(Bitmap image, String checkoutId, Error error) {
                super.onError(image, checkoutId, error);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // image cannot be properly scaled
                // "if height has to be scaled up, resulting width should be acceptable"
                wePay.storeSignatureImage(Bitmap.createBitmap(129, 32, Bitmap.Config.ALPHA_8), "checkout id", checkoutHandler);
            }
        });

        countDownLatch.await(50, TimeUnit.MILLISECONDS);

        Assert.assertTrue(checkoutHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidSignatureImageError(null), checkoutHandler.error);
    }

}
