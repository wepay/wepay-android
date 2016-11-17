package com.wepay.android.internal.mock;

import android.os.Handler;
import android.os.Looper;

import com.roam.roamreaderunifiedapi.ConfigurationManager;
import com.roam.roamreaderunifiedapi.DisplayControl;
import com.roam.roamreaderunifiedapi.KeyPadControl;
import com.roam.roamreaderunifiedapi.callback.DeviceResponseHandler;
import com.roam.roamreaderunifiedapi.callback.DeviceStatusHandler;
import com.roam.roamreaderunifiedapi.constants.Command;
import com.roam.roamreaderunifiedapi.constants.LanguageCode;
import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.roam.roamreaderunifiedapi.constants.ProgressMessage;
import com.roam.roamreaderunifiedapi.data.ApplicationIdentifier;
import com.roam.roamreaderunifiedapi.data.Device;
import com.roam.roamreaderunifiedapi.data.PublicKey;
import com.wepay.android.models.MockConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MockRoamConfigurationManager implements ConfigurationManager{
    private MockConfig mockConfig;
    private DeviceStatusHandler deviceStatusHandler;
    private static Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private void runOnMainThread(Runnable runnable) {
        mainThreadHandler.post(runnable);
    }

    public MockRoamConfigurationManager(MockConfig mockConfig, DeviceStatusHandler deviceStatusHandler) {
        this.mockConfig = mockConfig;
        this.deviceStatusHandler = deviceStatusHandler;
    }

    public MockRoamConfigurationManager setMockConfig(MockConfig mockConfig) {
        this.mockConfig = mockConfig;
        return this;
    }

    public MockRoamConfigurationManager setDeviceStatusHandler(DeviceStatusHandler deviceStatusHandler) {
        this.deviceStatusHandler = deviceStatusHandler;
        return this;
    }

    @Override
    public void setCommandTimeout(Integer integer) {

    }

    @Override
    public void getDeviceCapabilities(DeviceResponseHandler deviceResponseHandler) {
        final Map<Parameter, Object> res = new HashMap<>();
        res.put(Parameter.Command, Command.ReadCapabilities);
        res.put(Parameter.InterfaceDeviceSerialNumber, "S6RP350X50X-02?15271RP1000136500");

        final DeviceResponseHandler handler = deviceResponseHandler;
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                handler.onProgress(ProgressMessage.CommandSent, null);
                handler.onProgress(ProgressMessage.WaitingforDevice, null);
                handler.onResponse(res);
            }
        });
    }

    @Override
    public void setExpectedAmountDOL(List<Parameter> list) {

    }

    @Override
    public void setExpectedOnlineDOL(List<Parameter> list) {

    }

    @Override
    public void setExpectedResponseDOL(List<Parameter> list) {

    }

    @Override
    public void clearAIDSList(DeviceResponseHandler deviceResponseHandler) {
        final DeviceResponseHandler responseHandler = deviceResponseHandler;
        final Map<Parameter, Object> res = new HashMap<>();
        res.put(Parameter.Command, Command.ClearAIDsList);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                responseHandler.onResponse(res);
            }
        });
    }

    @Override
    public void clearPublicKeys(DeviceResponseHandler deviceResponseHandler) {
        final DeviceResponseHandler responseHandler = deviceResponseHandler;
        final Map<Parameter, Object> res = new HashMap<>();
        res.put(Parameter.Command, Command.ClearPublicKeys);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                responseHandler.onResponse(res);
            }
        });
    }

    @Override
    public void submitAIDList(Set<ApplicationIdentifier> set, DeviceResponseHandler deviceResponseHandler) {
        final DeviceResponseHandler responseHandler = deviceResponseHandler;
        final Map<Parameter, Object> res = new HashMap<>();
        res.put(Parameter.Command, Command.SubmitAIDsList);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                responseHandler.onResponse(res);
            }
        });
    }

    @Override
    public void submitPublicKey(PublicKey publicKey, DeviceResponseHandler deviceResponseHandler) {
        final DeviceResponseHandler responseHandler = deviceResponseHandler;
        final Map<Parameter, Object> res = new HashMap<>();
        res.put(Parameter.Command, Command.SubmitPublicKey);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                responseHandler.onResponse(res);
            }
        });
    }

    @Override
    public void setAmountDOL(List<Parameter> list, DeviceResponseHandler deviceResponseHandler) {
        final DeviceResponseHandler responseHandler = deviceResponseHandler;
        final Map<Parameter, Object> res = new HashMap<>();
        res.put(Parameter.Command, Command.ConfigureAmountDOLData);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                responseHandler.onResponse(res);
            }
        });
    }

    @Override
    public void setOnlineDOL(List<Parameter> list, DeviceResponseHandler deviceResponseHandler) {
        final DeviceResponseHandler responseHandler = deviceResponseHandler;
        final Map<Parameter, Object> res = new HashMap<>();
        res.put(Parameter.Command, Command.ConfigureOnlineDOLData);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                responseHandler.onResponse(res);
            }
        });
    }

    @Override
    public void setResponseDOL(List<Parameter> list, DeviceResponseHandler deviceResponseHandler) {
        final DeviceResponseHandler responseHandler = deviceResponseHandler;
        final Map<Parameter, Object> res = new HashMap<>();
        res.put(Parameter.Command, Command.ConfigureResponseDOLData);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                responseHandler.onResponse(res);
            }
        });
    }

    @Override
    public void setUserInterfaceOptions(Integer integer, LanguageCode languageCode, Byte aByte, Byte aByte1, DeviceResponseHandler deviceResponseHandler) {
        final DeviceResponseHandler responseHandler = deviceResponseHandler;
        final Map<Parameter, Object> res = new HashMap<>();
        res.put(Parameter.Command, Command.ConfigureUserInterfaceOptions);
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                responseHandler.onResponse(res);
            }
        });
    }

    @Override
    public Boolean activateDevice(Device device) {
        return null;
    }

    @Override
    public void generateBeep(DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public List<Device> getAvailableDevices() {
        return null;
    }

    @Override
    public void readVersion(DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void resetDevice(DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void retrieveKSN(DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void revokePublicKey(PublicKey publicKey, DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void setUserInterfaceOptions(DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public DisplayControl getDisplayControl() {
        return null;
    }

    @Override
    public KeyPadControl getKeypadControl() {
        return null;
    }

    @Override
    public void sendRawcommand(String s, DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void loadSessionKey(Integer integer, String s, String s1, String s2, String s3, String s4, DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void setEnergySaverModeTime(Integer integer, DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void setShutDownModeTime(Integer integer, DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void setUserInterfaceOptions(Integer integer, LanguageCode languageCode, Boolean aBoolean, List<LanguageCode> list, Byte aByte, Byte aByte1, Byte aByte2, Byte aByte3, DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void submitContactlessAIDList(Set<ApplicationIdentifier> set, DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void setContactlessOnlineDOL(List<Parameter> list, DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void setContactlessResponseDOL(List<Parameter> list, DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void setExpectedContactlessOnlineDOL(List<Parameter> list) {

    }

    @Override
    public void setExpectedContactlessResponseDOL(List<Parameter> list) {

    }

    @Override
    public void enableContactless(DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void disableContactless(DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void configureContactlessTransactionOptions(Boolean aBoolean, Boolean aBoolean1, Boolean aBoolean2, Boolean aBoolean3, Boolean aBoolean4, Boolean aBoolean5, Boolean aBoolean6, Boolean aBoolean7, Boolean aBoolean8, DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void configureContactlessTransactionOptions(Boolean aBoolean, Boolean aBoolean1, Boolean aBoolean2, Boolean aBoolean3, Boolean aBoolean4, Boolean aBoolean5, Boolean aBoolean6, Boolean aBoolean7, Boolean aBoolean8, Integer integer, DeviceResponseHandler deviceResponseHandler) {

    }
}
