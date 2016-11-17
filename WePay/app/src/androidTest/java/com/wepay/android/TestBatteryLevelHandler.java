package com.wepay.android;

import java.util.concurrent.CountDownLatch;

public class TestBatteryLevelHandler implements BatteryLevelHandler{
    public boolean onBatteryLevelCalled = false;
    public boolean onBatteryLevelErrorCalled = false;
    private CountDownLatch countDownLatch;

    public TestBatteryLevelHandler(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onBatteryLevel(int batteryLevel) {
        this.onBatteryLevelCalled = true;
    }

    @Override
    public void onBatteryLevelError(com.wepay.android.models.Error error) {
        this.onBatteryLevelErrorCalled = true;
    }
}
