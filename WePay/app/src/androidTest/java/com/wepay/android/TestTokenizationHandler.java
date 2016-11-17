package com.wepay.android;

import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

import java.util.concurrent.CountDownLatch;

public class TestTokenizationHandler implements TokenizationHandler {
    public boolean onSuccessCalled = false;
    public boolean onErrorCalled = false;
    public PaymentToken paymentToken;
    public Error error;
    private CountDownLatch countDownLatch;

    public TestTokenizationHandler(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onSuccess(PaymentInfo paymentInfo, PaymentToken token) {
        onSuccessCalled = true;
        this.paymentToken = token;
    }

    @Override
    public void onError(PaymentInfo paymentInfo, Error error) {
        onErrorCalled = true;
        this.error = error;
    }
}
