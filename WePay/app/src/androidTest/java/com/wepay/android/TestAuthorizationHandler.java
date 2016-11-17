package com.wepay.android;

import com.wepay.android.models.AuthorizationInfo;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class TestAuthorizationHandler implements AuthorizationHandler {
    public boolean onAuthorizationSuccessCalled = false;
    public boolean onAuthorizationErrorCalled = false;
    public boolean onEMVApplicationSelectionRequestedCalled = false;
    public PaymentInfo paymentInfo;
    public AuthorizationInfo authorizationInfo;
    public Error error;
    private CountDownLatch countDownLatch;

    public TestAuthorizationHandler(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onEMVApplicationSelectionRequested(ApplicationSelectionCallback callback, ArrayList<String> applications) {
        onEMVApplicationSelectionRequestedCalled = true;
        callback.useApplicationAtIndex(applications.size() - 1);
    }

    @Override
    public void onAuthorizationSuccess(PaymentInfo paymentInfo, AuthorizationInfo authorizationInfo) {
        onAuthorizationSuccessCalled = true;
        this.authorizationInfo = authorizationInfo;
        this.paymentInfo = paymentInfo;
    }

    @Override
    public void onAuthorizationError(PaymentInfo paymentInfo, Error error) {
        onAuthorizationErrorCalled = true;
        this.error = error;
    }
}
