package com.wepay.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.roam.roamreaderunifiedapi.constants.ErrorCode;
import com.wepay.android.enums.CalibrationResult;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.CurrencyCode;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.internal.mock.MockRoamDeviceManager;
import com.wepay.android.internal.mock.MockRoamTransactionManager;
import com.wepay.android.models.AuthorizationInfo;
import com.wepay.android.models.CalibrationParameters;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.MockConfig;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

import junit.framework.Assert;

import org.junit.After;
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
    private static final long WAIT_TIME_SHORT_MS = 3000;
    private static final long WAIT_TIME_MED_MS = 4000;
    private static final long WAIT_TIME_LONG_MS = 5000;
    private static final long CONNECTION_TIME_MS = 7000; // Connection timeout defined in IngenicoCardReaderManager
    private static final long DISCOVERY_TIME_MS = 1000; // Discovery time defined in MockRoamDeviceManager



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

    @After
    public void tearDown() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CONTEXT);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

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
                super.onStatusChange(status);

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

        // Waiting for DISCOVERY_TIME_MS + CONNECTION_TIME_MS + 1 second as buffer
        countDownLatch.await(DISCOVERY_TIME_MS + CONNECTION_TIME_MS + 1000, TimeUnit.MILLISECONDS);

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


        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertEquals(EMAIL, cardReaderHandler.paymentInfo.getEmail());
    }

    @Test
    public void testReaderResetRequest() throws InterruptedException {
        Config config = getConfig();
        config.setStopCardReaderAfterOperation(false);

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
				super.onStatusChange(status);

                if (status == CardReaderStatus.CONFIGURING_READER && this.onReaderResetRequestedCalled) {
                    onConfiguringReaderStatusChangeCalled = true;
                    countDownLatch.countDown();
                }
            }

            @Override
            public void onSuccess(PaymentInfo paymentInfo) {
                super.onSuccess(paymentInfo);
                wePay.startTransactionForReading(this);
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForReading(cardReaderHandler);
            }
        });

        countDownLatch.await(WAIT_TIME_LONG_MS, TimeUnit.MILLISECONDS);

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

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

        countDownLatch.await(WAIT_TIME_MED_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(authorizationHandler.onAuthorizationErrorCalled);
    }

    @Test
    public void testEMVApplicationSelectionSuccessTransactionTokenizing() throws InterruptedException {
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onEMVApplicationSelectionRequestedCalled);

        // Asserting the right application (last one among four to choose from) was selected
        String paymentDescription = authorizationHandler.paymentInfo == null ? null : authorizationHandler.paymentInfo.getPaymentDescription();
        String lastFourDigitsOfPAN = paymentDescription == null ? null : paymentDescription.substring(paymentDescription.length() - 4);
        Assert.assertEquals("4444", lastFourDigitsOfPAN);
    }

    @Test
    public void testEMVApplicationSelectionSuccessTransactionReading() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP).setMultipleEMVApplication(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(null) {
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onEMVApplicationSelectionRequestedCalled);

        // Asserting the right application (last one among four to choose from) was selected
        String paymentDescription = cardReaderHandler.paymentInfo == null ? null : cardReaderHandler.paymentInfo.getPaymentDescription();
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
            public void onEMVApplicationSelectionRequested(ApplicationSelectionCallback callback, ArrayList<String> applications) {
                callback.useApplicationAtIndex(-1);
            }

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        // 24.61 is the magic number that will lead to authorization success, used in TestCardReaderHandler.
        Assert.assertEquals(new BigDecimal("24.61"), authorizationHandler.authorizationInfo.getAuthorizedAmount());
    }

    // Possible errors returned by validateAuthInfo() method in IngenicoCardReaderManager class
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidTransactionAmountError(), cardReaderHandler.error);
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidTransactionAmountError(), cardReaderHandler.error);
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidTransactionAmountError(), cardReaderHandler.error);
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidTransactionAccountIDError(), cardReaderHandler.error);
    }

    @Test
    public void testStopCardReader() throws InterruptedException {
        Config config = getConfig();
        config.setStopCardReaderAfterOperation(false);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onTransactionInfoRequested(CardReaderTransactionInfoCallback callback) {
                // This callback is overridden with empty body so startCardReaderForReading()
                // is stuck here and not calling stopCardReader()
            }

            @Override
            public void onStatusChange(CardReaderStatus status) {
                super.onStatusChange(status);

                if (status == CardReaderStatus.WAITING_FOR_CARD) {
                    wePay.stopCardReader();
                } else if (status == CardReaderStatus.STOPPED) {
                    onStoppedCalled = true;
                    countDownLatch.countDown();
                }
            }
        };

        Runnable finalStatusRunnable = new Runnable() {
            @Override
            public void run() {
                if (!cardReaderHandler.notConnectedStatusChangeCalled) {
                    countDownLatch.countDown();
                }
            }
        };

        this.handler.postDelayed(finalStatusRunnable, WAIT_TIME_MED_MS);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForReading(cardReaderHandler);
                // Calling startCardReaderForReading() is needed to pass in the CardReaderHandler instance
                // so the onStatusChange() callback can be called with `STOPPED` status
            }
        });

        countDownLatch.await(WAIT_TIME_LONG_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onStoppedCalled);
        Assert.assertEquals(CardReaderStatus.STOPPED, cardReaderHandler.mostRecentStatus);
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(calibrationHandler.onCompleteCalled);
    }

    @Test
    public void testBatteryInfoSuccess() throws InterruptedException {
        Config config = getConfig();

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch);
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
                wePay.getCardReaderBatteryLevel(cardReaderHandler, batteryLevelHandler);
            }
        });

        countDownLatch.await(WAIT_TIME_LONG_MS, TimeUnit.MILLISECONDS);
        Assert.assertTrue(batteryLevelHandler.onBatteryLevelCalled);
        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
    }

    @Test
    public void testBatteryInfoError() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setBatteryLevelError(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch);
        final TestBatteryLevelHandler batteryLevelHandler = new TestBatteryLevelHandler(countDownLatch) {
            @Override
            public void onBatteryLevelError(Error error) {
                super.onBatteryLevelError(error);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.getCardReaderBatteryLevel(cardReaderHandler, batteryLevelHandler);
            }
        });

        countDownLatch.await(WAIT_TIME_LONG_MS, TimeUnit.MILLISECONDS);
        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertTrue(batteryLevelHandler.onBatteryLevelErrorCalled);
    }

    @Test
    public void testBatteryInfoSuccessAfterTransaction_StopAfterOperationFalse() throws InterruptedException {
        Config config = getConfig();
        config.setStopCardReaderAfterOperation(false);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final TestBatteryLevelHandler batteryLevelHandler = new TestBatteryLevelHandler(countDownLatch) {
            @Override
            public void onBatteryLevel(int batteryLevel) {
                super.onBatteryLevel(batteryLevel);
                countDownLatch.countDown();
            }
        };
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onSuccess(PaymentInfo paymentInfo) {
                super.onSuccess(paymentInfo);
                countDownLatch.countDown();
                wePay.getCardReaderBatteryLevel(this, batteryLevelHandler);
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, new TestTokenizationHandler(countDownLatch), new TestAuthorizationHandler(countDownLatch));
            }
        });

        countDownLatch.await(WAIT_TIME_MED_MS, TimeUnit.MILLISECONDS);
        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertTrue(cardReaderHandler.onSuccessCalled);
        Assert.assertFalse(cardReaderHandler.onErrorCalled);
        Assert.assertFalse(batteryLevelHandler.onBatteryLevelErrorCalled);
        Assert.assertTrue(batteryLevelHandler.onBatteryLevelCalled);
    }

    @Test
    public void testBatteryInfoErrorAfterTransaction_StopAfterOperationFalse() throws InterruptedException {
        Config config = getConfig();
        config.setStopCardReaderAfterOperation(false);
        config.getMockConfig().setBatteryLevelError(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final TestBatteryLevelHandler batteryLevelHandler = new TestBatteryLevelHandler(countDownLatch) {
            @Override
            public void onBatteryLevelError(Error error) {
                super.onBatteryLevelError(error);
                countDownLatch.countDown();
            }
        };
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onSuccess(PaymentInfo paymentInfo) {
                super.onSuccess(paymentInfo);
                countDownLatch.countDown();
                wePay.getCardReaderBatteryLevel(this, batteryLevelHandler);
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, new TestTokenizationHandler(countDownLatch), new TestAuthorizationHandler(countDownLatch));
            }
        });

        countDownLatch.await(WAIT_TIME_MED_MS, TimeUnit.MILLISECONDS);
        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertTrue(cardReaderHandler.onSuccessCalled);
        Assert.assertFalse(cardReaderHandler.onErrorCalled);
        Assert.assertTrue(batteryLevelHandler.onBatteryLevelErrorCalled);
        Assert.assertFalse(batteryLevelHandler.onBatteryLevelCalled);
    }

    @Test
    public void testBatteryInfoSuccessAfterTransaction_StopAfterOperationTrue() throws InterruptedException {
        Config config = getConfig();
        config.setStopCardReaderAfterOperation(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final TestBatteryLevelHandler batteryLevelHandler = new TestBatteryLevelHandler(countDownLatch) {
            @Override
            public void onBatteryLevel(int batteryLevel) {
                super.onBatteryLevel(batteryLevel);
                countDownLatch.countDown();
            }
        };
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onSuccess(PaymentInfo paymentInfo) {
                super.onSuccess(paymentInfo);
                countDownLatch.countDown();
                wePay.getCardReaderBatteryLevel(this, batteryLevelHandler);
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, new TestTokenizationHandler(countDownLatch), new TestAuthorizationHandler(countDownLatch));
            }
        });
        // Since configured to stop, we will go through discovery twice. The Long wait time is to
        // account for the successful transaction time.
        countDownLatch.await(DISCOVERY_TIME_MS + DISCOVERY_TIME_MS + WAIT_TIME_LONG_MS, TimeUnit.MILLISECONDS);
        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertTrue(cardReaderHandler.onSuccessCalled);
        Assert.assertFalse(cardReaderHandler.onErrorCalled);
        Assert.assertFalse(batteryLevelHandler.onBatteryLevelErrorCalled);
        Assert.assertTrue(batteryLevelHandler.onBatteryLevelCalled);
    }

    @Test
    public void testBatteryInfoErrorAfterTransaction_StopAfterOperationTrue() throws InterruptedException {
        Config config = getConfig();
        config.setStopCardReaderAfterOperation(true);
        config.getMockConfig().setBatteryLevelError(true);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final TestBatteryLevelHandler batteryLevelHandler = new TestBatteryLevelHandler(countDownLatch) {
            @Override
            public void onBatteryLevelError(Error error) {
                super.onBatteryLevelError(error);
                countDownLatch.countDown();
            }
        };
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onSuccess(PaymentInfo paymentInfo) {
                super.onSuccess(paymentInfo);
                countDownLatch.countDown();
                wePay.getCardReaderBatteryLevel(this, batteryLevelHandler);
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForTokenizing(cardReaderHandler, new TestTokenizationHandler(countDownLatch), new TestAuthorizationHandler(countDownLatch));
            }
        });

        // Since configured to stop, we will go through discovery twice. The Long wait time is to
        // account for the successful transaction time.
        countDownLatch.await(DISCOVERY_TIME_MS + DISCOVERY_TIME_MS + WAIT_TIME_LONG_MS, TimeUnit.MILLISECONDS);
        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertTrue(cardReaderHandler.onSuccessCalled);
        Assert.assertFalse(cardReaderHandler.onErrorCalled);
        Assert.assertTrue(batteryLevelHandler.onBatteryLevelErrorCalled);
        Assert.assertFalse(batteryLevelHandler.onBatteryLevelCalled);
    }

    @Test
    public void testBatteryLevelTooLow() throws InterruptedException {
        Config config = getConfig();
        MockRoamDeviceManager deviceManager = MockRoamDeviceManager.getDeviceManager();
        final MockRoamTransactionManager transactionManager = (MockRoamTransactionManager) deviceManager.getTransactionManager();
        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        config.getMockConfig().setCardReadFailure(true);

        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onStatusChange(CardReaderStatus status) {
                super.onStatusChange(status);
                if (status.equals(CardReaderStatus.CHECKING_READER)) {
                    transactionManager.mockCommandErrorCode = ErrorCode.BatteryTooLowError;
                }
            }

            @Override
            public void onError(Error error) {
                super.onError(error);
                if (error.equals(Error.getCardReaderBatteryTooLowError())) {
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

        countDownLatch.await(WAIT_TIME_MED_MS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(0, countDownLatch.getCount());
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(checkoutHandler.onErrorCalled);
        Assert.assertEquals(Error.getInvalidSignatureImageError(null), checkoutHandler.error);
    }

    @Test
    public void testNegativeCardReaderSelectionIndex() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onCardReaderSelection(CardReaderSelectionCallback callback, ArrayList<String> devices) {
                onCardReaderSelectionCalled = true;
                callback.useCardReaderAtIndex(-1);
            }

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertTrue(cardReaderHandler.onErrorCalled);
    }
    @Test
    public void testOutOfBoundsCardReaderSelectionIndex() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onCardReaderSelection(CardReaderSelectionCallback callback, ArrayList<String> devices) {
                onCardReaderSelectionCalled = true;
                callback.useCardReaderAtIndex(devices.size());
            }

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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertTrue(cardReaderHandler.onErrorCalled);
    }

    @Test
    public void  testCardReaderSelectionRestart() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP);

        final ArrayList<CardReaderStatus> statuses = new ArrayList<>();
        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onCardReaderSelection(CardReaderSelectionCallback callback, ArrayList<String> devices) {
                callback.useCardReaderAtIndex(-1);
                onCardReaderSelectionCalled = true;
            }

            @Override
            public void onStatusChange(CardReaderStatus status) {
                super.onStatusChange(status);
                statuses.add(status);
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForReading(cardReaderHandler);
            }
        });

        countDownLatch.await(WAIT_TIME_LONG_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertFalse(cardReaderHandler.onSuccessCalled);
        Assert.assertEquals(statuses.size(), 1);
        Assert.assertEquals(statuses.get(0), CardReaderStatus.SEARCHING_FOR_READER);
    }

    @Test
    public void testCardReaderSelectionNoRestart() throws InterruptedException {
        Config config = getConfig();
        config.setRestartTransactionAfterOtherErrors(true);
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP);

        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            public boolean calledOnce = false;
            @Override
            public void onCardReaderSelection(CardReaderSelectionCallback callback, ArrayList<String> devices) {
                if (!calledOnce) {
                    callback.useCardReaderAtIndex(-1);
                    calledOnce = true;
                } else {
                    callback.useCardReaderAtIndex(0);
                    onCardReaderSelectionCalled = true;
                }
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

        countDownLatch.await(WAIT_TIME_LONG_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertTrue(cardReaderHandler.onErrorCalled);
        Assert.assertTrue(cardReaderHandler.onSuccessCalled);
        Assert.assertNotNull(cardReaderHandler.paymentInfo);
    }

    @Test
    public void testCardReaderSelectionStop() throws InterruptedException {
        Config config = getConfig();
        config.getMockConfig().setMockPaymentMethod(PaymentMethod.DIP);

        final ArrayList<CardReaderStatus> statuses = new ArrayList<>();
        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onCardReaderSelection(CardReaderSelectionCallback callback, ArrayList<String> devices) {
                onCardReaderSelectionCalled = true;
                wePay.stopCardReader();
                countDownLatch.countDown();
            }

            @Override
            public void onStatusChange(CardReaderStatus status) {
                super.onStatusChange(status);
                statuses.add(status);
                countDownLatch.countDown();
            }
        };

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                wePay.startTransactionForReading(cardReaderHandler);
            }
        });

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertEquals(statuses.size(), 2);
        Assert.assertEquals(statuses.get(0), CardReaderStatus.SEARCHING_FOR_READER);
        Assert.assertEquals(statuses.get(1), CardReaderStatus.STOPPED);
    }

    @Test
    public void testCardReaderSearchesUntilDiscovered() throws InterruptedException {
        final Config config = getConfig();
        config.getMockConfig().setMockCardReaderDetected(false);

        final ArrayList<CardReaderStatus> statuses = new ArrayList<>();
        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onCardReaderSelection(CardReaderSelectionCallback callback, ArrayList<String> devices) {
                super.onCardReaderSelection(callback, devices);
            }

            @Override
            public void onStatusChange(CardReaderStatus status) {
                super.onStatusChange(status);
                statuses.add(status);
                if (status == CardReaderStatus.NOT_CONNECTED) {
                    countDownLatch.countDown();
                    config.getMockConfig().setMockCardReaderDetected(true);
                } else if (status == CardReaderStatus.CONNECTED) {
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

        // Waiting for CONNECTION_TIME_MS + DISCOVERY_TIME + 1 seconds as buffer
        // We are waiting for the first attempt at connection to fail, then expecting
        // the second attempt to succeed.
        countDownLatch.await(CONNECTION_TIME_MS + DISCOVERY_TIME_MS + 1000, TimeUnit.MILLISECONDS);

        Assert.assertTrue(cardReaderHandler.onCardReaderSelectionCalled);
        Assert.assertTrue(statuses.size() >= 3);
        Assert.assertEquals(statuses.get(0), CardReaderStatus.SEARCHING_FOR_READER);
        Assert.assertEquals(statuses.get(1), CardReaderStatus.NOT_CONNECTED);
        Assert.assertEquals(statuses.get(2), CardReaderStatus.CONNECTED);
    }

	@Test
    public void testCheckRememberedCardReaderOnConnection() throws InterruptedException {
        final Config config = getConfig();
        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onSuccess(PaymentInfo paymentInfo) {
                super.onSuccess(paymentInfo);

                countDownLatch.countDown();
            }

            @Override
            public void onStatusChange(CardReaderStatus status) {
                super.onStatusChange(status);

                if (status == CardReaderStatus.CONNECTED) {
                    org.junit.Assert.assertEquals(wePay.getRememberedCardReader(), "AUDIOJACK");
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testForgetCardReaderOnConnection() throws InterruptedException {
        final Config config = getConfig();
        final WePay wePay = new WePay(config);
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final TestCardReaderHandler cardReaderHandler = new TestCardReaderHandler(countDownLatch) {
            @Override
            public void onSuccess(PaymentInfo paymentInfo) {
                super.onSuccess(paymentInfo);
                org.junit.Assert.assertEquals(wePay.getRememberedCardReader(), null);
                countDownLatch.countDown();
            }

            @Override
            public void onStatusChange(CardReaderStatus status) {
                super.onStatusChange(status);

                if (status == CardReaderStatus.CONNECTED) {
                    // Forget the card reader after it's been remembered.
                    wePay.forgetRememberedCardReader();
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

        countDownLatch.await(WAIT_TIME_SHORT_MS, TimeUnit.MILLISECONDS);
    }
}
