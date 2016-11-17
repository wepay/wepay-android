package com.wepay.android.internal.CardReader.DeviceHelpers;

import android.os.Handler;
import android.util.Log;

import com.roam.roamreaderunifiedapi.DeviceManager;
import com.roam.roamreaderunifiedapi.RoamReaderUnifiedAPI;
import com.roam.roamreaderunifiedapi.callback.DeviceResponseHandler;
import com.roam.roamreaderunifiedapi.callback.DeviceStatusHandler;
import com.roam.roamreaderunifiedapi.constants.Command;
import com.roam.roamreaderunifiedapi.constants.DeviceType;
import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.roam.roamreaderunifiedapi.constants.ProgressMessage;
import com.roam.roamreaderunifiedapi.constants.ResponseCode;
import com.roam.roamreaderunifiedapi.data.CalibrationParameters;
import com.wepay.android.BatteryLevelHandler;
import com.wepay.android.internal.mock.MockRoamDeviceManager;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.MockConfig;

import java.util.Map;

public class BatteryHelper implements DeviceResponseHandler, DeviceStatusHandler {

    private static final int RP350X_CONNECTION_TIME_SEC = 7;
    private static final int RP350X_CONNECTION_TIME_MS = RP350X_CONNECTION_TIME_SEC * 1000;

    private DeviceManager roamDeviceManager = null;
    private BatteryLevelHandler batteryLevelHandler = null;
    private Handler readerInformNotConnectedHandler = new Handler();
    private Runnable readerInformNotConnectedRunnable = null;

    public void getBatteryLevel(final BatteryLevelHandler batteryLevelHandler, final Config config) {
        if (batteryLevelHandler == null) {
            return;
        } else {
            this.batteryLevelHandler = batteryLevelHandler;
        }

        MockConfig mockConfig = config.getMockConfig();
        if (mockConfig != null && mockConfig.isUseMockCardReader()) {
            this.roamDeviceManager = MockRoamDeviceManager.getDeviceManager();
            ((MockRoamDeviceManager) this.roamDeviceManager).setMockConfig(mockConfig);
        } else {
            this.roamDeviceManager = RoamReaderUnifiedAPI.getDeviceManager(DeviceType.RP350x);
        }

        // initialize the roam device manager
        CalibrationHelper ch = new CalibrationHelper();
        CalibrationParameters params = ch.getCalibrationParams(config);
        if (params != null) {
            this.roamDeviceManager.initialize(config.getContext(), this, params);
        } else {
            this.roamDeviceManager.initialize(config.getContext(), this);
        }

        this.startWaitingForReader();
    }


    @Override
    public void onProgress(ProgressMessage message, String additionalMessage) {
        switch(message) {
            case CommandSent:
                //do nothing
                break;
            default:
                Log.d("wepay_sdk", "BatteryHelper.onProgress ignoring message: " + message.toString() + " - " + additionalMessage);
                // nothing to do here
        }
    }

    @Override
    public void onResponse(Map<Parameter, Object> data) {
        Log.d("wepay_sdk", "Command response: \n" + data.toString());

        Command cmd = (Command) data.get(Parameter.Command);
        ResponseCode responseCode = (ResponseCode) data.get(Parameter.ResponseCode);

        if (responseCode == ResponseCode.Error) {
            this.batteryLevelHandler.onBatteryLevelError(Error.getFailedToGetBatteryLevelError());
        } else {
            switch (cmd) {
                case BatteryInfo:
                    int batteryLevel = (int) data.get(Parameter.BatteryLevel);
                    this.batteryLevelHandler.onBatteryLevel(batteryLevel);
                    break;
                default:
                    Log.d("wepay_sdk","BatteryHelper.onResponse unexpected command :" + cmd.toString());
                    this.batteryLevelHandler.onBatteryLevelError(Error.getFailedToGetBatteryLevelError());
                    break;
            }
        }

        cleanup();
    }

    private void startWaitingForReader() {
        // cancel previous timer if it exists
        if (this.readerInformNotConnectedHandler != null) {
            this.readerInformNotConnectedHandler.removeCallbacks(this.readerInformNotConnectedRunnable);
        }

        // Wait a few seconds for the reader to be detected, otherwise return error
        this.readerInformNotConnectedRunnable = new Runnable() {
            @Override
            public void run() {
                if (batteryLevelHandler != null) {
                    batteryLevelHandler.onBatteryLevelError(Error.getCardReaderNotConnectedError());
                }

                cleanup();
            }
        };

        this.readerInformNotConnectedHandler.postDelayed(this.readerInformNotConnectedRunnable, RP350X_CONNECTION_TIME_MS);
    }

    private void stopWaitingForReader() {
        // cancel previous timer if it exists
        if (this.readerInformNotConnectedHandler != null) {
            this.readerInformNotConnectedHandler.removeCallbacks(this.readerInformNotConnectedRunnable);

            this.readerInformNotConnectedRunnable = null;
            this.readerInformNotConnectedHandler = null;
        }
    }

    private void cleanup() {
        this.stopWaitingForReader();

        this.batteryLevelHandler = null;
        this.roamDeviceManager.release();
        this.roamDeviceManager = null;
    }

    @Override
    public void onConnected() {
        this.stopWaitingForReader();

        if (this.roamDeviceManager != null) {
            this.roamDeviceManager.getBatteryStatus(this);
        }
    }

    @Override
    public void onDisconnected() {
        if (this.batteryLevelHandler != null) {
            this.batteryLevelHandler.onBatteryLevelError(Error.getCardReaderNotConnectedError());
            cleanup();
        }
    }

    @Override
    public void onError(String s) {
        Log.d("wepay_sdk","BatteryHelper.onError :" +s);
        if (this.batteryLevelHandler != null) {
            this.batteryLevelHandler.onBatteryLevelError(Error.getCardReaderUnknownError());
            cleanup();
        }
    }
}
