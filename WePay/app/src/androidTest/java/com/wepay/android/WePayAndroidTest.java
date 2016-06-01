package com.wepay.android;

import android.content.Context;
import android.location.Address;
import android.test.InstrumentationTestCase;

import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

import java.util.Locale;


/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */


public class WePayAndroidTest extends InstrumentationTestCase {

    private WePay wepay = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Initialize WePay

        String clientId = "171482";
        Context context = this.getInstrumentation().getContext();
        String environment = Config.ENVIRONMENT_STAGE;

        Config config = new Config(context, clientId, environment).setUseLocation(true);

        this.wepay = new WePay(config);

    }

    public void test_valid_tokenize() throws Throwable {

        final TokenizationTester tester = new TokenizationTester();
        Address address = new Address(Locale.getDefault());
        address.setAddressLine(0, "380 Portage ave");
        address.setLocality("Palo Alto");
        address.setPostalCode("94306");
        address.setCountryCode("US");

        final PaymentInfo paymentInfo = new PaymentInfo("Android", "Tester", "a@b.com",
                "Visa xxxx-1234", address,
                address, PaymentMethod.MANUAL,
                "4242424242424242", "123", "01", "18", true);

        runTestOnUiThread(new Runnable() {

            @Override
            public void run() {
                wepay.tokenize(paymentInfo, tester);
            }
        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNotNull(tester.getToken());
    }

    public void test_invalid_tokenize() throws Throwable {

        final TokenizationTester tester = new TokenizationTester();
        Address address = new Address(Locale.getDefault());
        address.setAddressLine(0, "380 Portage ave");
        address.setLocality("Palo Alto");
        address.setPostalCode("94306");
        address.setCountryCode("US");

        final PaymentInfo paymentInfo = new PaymentInfo("Android", "Tester", "a@b.com",
                "Visa xxxx-1234", address,
                address, PaymentMethod.MANUAL,
                "424242442424242", "123", "01", "18", true);

        runTestOnUiThread(new Runnable() {

            @Override
            public void run() {
                wepay.tokenize(paymentInfo, tester);
            }
        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNotNull(tester.getError());
    }

    class TokenizationTester implements TokenizationHandler {

        private PaymentToken token = null;
        private Error error = null;

        /**
         * Gets called when a tokenization calls succeeds.
         *
         * @param paymentInfo the payment info passed to the tokenization call
         * @param token       the token representing the payment info
         */
        @Override
        public void onSuccess(PaymentInfo paymentInfo, PaymentToken token) {
            this.token = token;
        }


        /**
         * Gets called when a tokenization call fails.
         *
         * @param paymentInfo the payment info
         * @param error       the error due to which tokenization failed
         */
        @Override
        public void onError(PaymentInfo paymentInfo, com.wepay.android.models.Error error) {
            this.error = error;
        }

        public PaymentToken getToken() {
            return this.token;
        }
        public Error getError() {
            return this.error;
        }
    }
}