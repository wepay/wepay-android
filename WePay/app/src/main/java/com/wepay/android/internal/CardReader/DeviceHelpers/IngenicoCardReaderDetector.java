package com.wepay.android.internal.CardReader.DeviceHelpers;

import android.os.Handler;
import android.util.Log;

import com.roam.roamreaderunifiedapi.DeviceManager;
import com.roam.roamreaderunifiedapi.RoamReaderUnifiedAPI;
import com.roam.roamreaderunifiedapi.callback.DeviceStatusHandler;
import com.roam.roamreaderunifiedapi.callback.SearchListener;
import com.roam.roamreaderunifiedapi.constants.DeviceType;
import com.roam.roamreaderunifiedapi.data.CalibrationParameters;
import com.roam.roamreaderunifiedapi.data.Device;
import com.wepay.android.internal.mock.MockRoamDeviceManager;
import com.wepay.android.models.Config;
import com.wepay.android.models.MockConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for automatically determining which Roam DeviceManager is attached.
 *
 * Currently supported devices:
 *      - RP350X (headphone jack)
 *      - Moby3000 (Bluetooth)
 *
 * When findFirstAvailableCardReader is called, IngenicoCardReaderDetector tries to both initialize the
 * headphone jack DeviceManager and scan for a Bluetooth device.
 *      - If a supported headphone jack device is plugged in, we pass that DeviceManager up
 *        to the delegate (see onConnected).
 *      - If a supported Bluetooth device is within range and Roam discovers it, we initialize
 *        the Bluetooth DeviceManager (see onDeviceDiscovered). At this point the flow is identical
 *        to the headphone jack flow: after initialization we pass the DeviceManager up to
 *        the delegate (see onConnected).
 *      - If no devices are found in the headphone jack or in Bluetooth scanning range, detection
 *        will time out and the delegate will be notified (see usage of startTimeCounter).
 */

public class IngenicoCardReaderDetector implements DeviceStatusHandler, SearchListener {

    /** Timeout the search after 8 seconds  */
    private static final long TIMEOUT_DEVICE_SEARCH_MS = 8000L;

    private DeviceManager rp350xRoamDeviceManager = null;
    private DeviceManager moby3000RoamDeviceManager = null;
    private MockRoamDeviceManager mockRoamDeviceManager = null;
    private List<DeviceManager> supportedDeviceManagers;

    private Config config = null;
    private CalibrationParameters calibrationParameters = null;
    private CardReaderDetectionDelegate delegate = null;

    private Handler deviceDetectionTimeoutHandler = new Handler();

    private Boolean isDeviceDiscovered = false;

    public IngenicoCardReaderDetector() {
        this.rp350xRoamDeviceManager = RoamReaderUnifiedAPI.getDeviceManager(DeviceType.RP350x);
        this.moby3000RoamDeviceManager = RoamReaderUnifiedAPI.getDeviceManager(DeviceType.MOBY3000);
        this.mockRoamDeviceManager = MockRoamDeviceManager.getDeviceManager();

        this.supportedDeviceManagers = new ArrayList<>(3);
        this.supportedDeviceManagers.add(this.rp350xRoamDeviceManager);
        this.supportedDeviceManagers.add(this.moby3000RoamDeviceManager);
        this.supportedDeviceManagers.add(this.mockRoamDeviceManager);
    }

    public void findFirstAvailableCardReader(Config config, CalibrationParameters calibrationParameters, CardReaderDetectionDelegate detectionDelegate) {
        MockConfig mockConfig = config.getMockConfig();

        this.config = config;
        this.calibrationParameters = calibrationParameters;
        this.delegate = detectionDelegate;

        if (mockConfig != null && mockConfig.isUseMockCardReader()) {
            this.findFirstAvailableDeviceMock(mockConfig);
        } else {
            this.findFirstAvailableDeviceInternal(config, detectionDelegate);
        }
    }

    private void findFirstAvailableDeviceInternal(Config config,
                                                  CardReaderDetectionDelegate detectionDelegate) {
        DeviceManager existingDeviceManager = getReadyDeviceManager(this.supportedDeviceManagers);

        this.cancelAllDeviceManagerSearches(this.supportedDeviceManagers);

        if (existingDeviceManager == null) {
            this.initializeDeviceManager(this.rp350xRoamDeviceManager);
            this.moby3000RoamDeviceManager.searchDevices(config.getContext(), false, TIMEOUT_DEVICE_SEARCH_MS, this);

            stopTimeCounter(this.deviceDetectionTimeoutHandler);
            startTimeCounter(this.deviceDetectionTimeoutHandler, TIMEOUT_DEVICE_SEARCH_MS);
        } else {
            // We already have an existing DeviceManager that's ready to go, so pass that to the delegate.
            detectionDelegate.onCardReaderManagerDetected(existingDeviceManager);
        }
    }

    private void findFirstAvailableDeviceMock(MockConfig mockConfig) {
        this.mockRoamDeviceManager.setMockConfig(mockConfig);
        this.initializeDeviceManager(this.mockRoamDeviceManager);
    }

    private void initializeDeviceManager(DeviceManager deviceManager) {
        final DeviceStatusHandler statusHandler = this;

        if (this.calibrationParameters != null) {
            deviceManager.initialize(this.config.getContext(), statusHandler, this.calibrationParameters);
        } else {
            deviceManager.initialize(this.config.getContext(), statusHandler);
        }
    }

    private DeviceManager getReadyDeviceManager(List<DeviceManager> possibleDeviceManagers) {
        DeviceManager result = null;

        for (DeviceManager deviceManager : possibleDeviceManagers) {
            if (deviceManager.isReady()) {
                result = deviceManager;
                break;
            }
        }

        return result;
    }

    private Boolean isAnyDeviceManagerReady() {
        return getReadyDeviceManager(this.supportedDeviceManagers) != null;
    }

    private void cancelAllDeviceManagerSearches(List<DeviceManager> possibleDeviceManagers) {
        for (DeviceManager deviceManager : possibleDeviceManagers) {
            deviceManager.cancelSearch();
        }
    }

    private void startTimeCounter(Handler counterHandler, long timeLength) {
        // Wait a few seconds for the reader to be detected, otherwise announce not connected
        Runnable counterRunnable = new Runnable() {
            @Override
            public void run() {
                delegate.onCardReaderDetectionTimeout();
                cancelAllDeviceManagerSearches(supportedDeviceManagers);
            }
        };

        counterHandler.postDelayed(counterRunnable, timeLength);
    }

    private void stopTimeCounter(Handler counterHandler) {
        counterHandler.removeCallbacksAndMessages(null);
    }

    /** DeviceStatusHandler */

    @Override
    public void onConnected() {
        // This callback will be invoked once Roam considers some device to be considered connected.
        Log.d("wepay_sdk", "detector connected a device");

        DeviceManager foundDeviceManager = getReadyDeviceManager(this.supportedDeviceManagers);

        if (foundDeviceManager == null) {
            // This should never happen unless we invoke initialize() on a Roam device manager of an
            // unsupported type.
            Log.e("wepay_sdk", "unknown card reader device connected");
        } else {
            this.cancelAllDeviceManagerSearches(this.supportedDeviceManagers);
            this.stopTimeCounter(this.deviceDetectionTimeoutHandler);
            this.delegate.onCardReaderManagerDetected(foundDeviceManager);
        }
    }

    @Override
    public void onDisconnected() {
        Log.e("wepay_sdk", "device detection: onDisconnected");
    }

    @Override
    public void onError(String message) {
        Log.e("wepay_sdk", "device detection: encountered roam error: " + message);

        this.delegate.onCardReaderDetectionFailed("Error initializing device. " + message);
    }


    /** SearchListener */

    @Override
    public void onDeviceDiscovered(Device device) {
        Log.d("wepay_sdk", "onDeviceDiscovered " + device.getDeviceType().toString());

        String name = device == null ? null : device.getName();
        Boolean isMoby = name == null ? false : name.startsWith("MOB30");

        if (isMoby && !this.moby3000RoamDeviceManager.isReady()) {
            Log.d("wepay_sdk", "initializing discovered device");

            this.isDeviceDiscovered = true;
            this.cancelAllDeviceManagerSearches(this.supportedDeviceManagers);
            this.stopTimeCounter(this.deviceDetectionTimeoutHandler);

            this.moby3000RoamDeviceManager.getConfigurationManager().activateDevice(device);

            initializeDeviceManager(this.moby3000RoamDeviceManager);
        }
    }

    @Override
    public void onDiscoveryComplete() {
        Log.d("wepay_sdk", "onDiscoveryComplete");

        if (!this.isDeviceDiscovered && !isAnyDeviceManagerReady()) {
            this.delegate.onCardReaderDetectionFailed("Unable to find any supported Bluetooth devices");
        }
    }

    public interface CardReaderDetectionDelegate {
        void onCardReaderManagerDetected(DeviceManager roamDeviceManager);
        void onCardReaderDetectionTimeout();
        void onCardReaderDetectionFailed(String message);
    }
}
