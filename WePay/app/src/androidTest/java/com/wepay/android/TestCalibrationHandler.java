package com.wepay.android;

import com.wepay.android.enums.CalibrationResult;
import com.wepay.android.models.CalibrationParameters;

import java.util.concurrent.CountDownLatch;

public class TestCalibrationHandler implements CalibrationHandler {
    public boolean onCompleteCalled = false;
    private CountDownLatch countDownLatch;

    public TestCalibrationHandler(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onProgress(double progress) {

    }

    @Override
    public void onComplete(CalibrationResult result, CalibrationParameters params) {
        onCompleteCalled = true;
    }
}
