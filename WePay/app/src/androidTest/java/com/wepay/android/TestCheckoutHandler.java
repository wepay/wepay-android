package com.wepay.android;

import android.graphics.Bitmap;

import com.wepay.android.models.Error;

import java.util.concurrent.CountDownLatch;

public class TestCheckoutHandler implements CheckoutHandler {
    boolean onSuccessCalled = false;
    boolean onErrorCalled = false;
    Error error;
    private CountDownLatch countDownLatch;

    public TestCheckoutHandler(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onSuccess(String signatureUrl, String checkoutId) {
        onSuccessCalled = true;
    }

    @Override
    public void onError(Bitmap image, String checkoutId, Error error) {
        onErrorCalled = true;
        this.error = error;
    }
}
