package com.wepay.android.internal.CardReader.DeviceHelpers;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

import com.roam.roamreaderunifiedapi.DeviceManager;
import com.roam.roamreaderunifiedapi.RoamReaderUnifiedAPI;
import com.roam.roamreaderunifiedapi.callback.DeviceStatusHandler;
import com.roam.roamreaderunifiedapi.callback.SearchListener;
import com.roam.roamreaderunifiedapi.constants.DeviceType;
import com.roam.roamreaderunifiedapi.data.CalibrationParameters;
import com.roam.roamreaderunifiedapi.data.Device;
import com.wepay.android.internal.SharedPreferencesHelper;
import com.wepay.android.internal.mock.MockRoamDeviceManager;
import com.wepay.android.models.Config;
import com.wepay.android.models.MockConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for detecting which (if any) Roam Devices are available.
 *
 * Currently supported devices:
 *      - RP350X (headphone jack)
 *      - Moby3000 (Bluetooth)
 *
 * When findAvailableCardReaders is called, IngenicoCardReaderDetector scans for devices
 * connected through the headphone jack or available via bluetooth.
 *      - If any headphone jack device is plugged in, we add it to the list of discovered devices.
 *      - If any Moby3000 Bluetooth device is within range and discovered, we add it to the list of
 *        discovered devices.
 *      - If no devices are found in the headphone jack or in Bluetooth scanning range, detection
 *        will time out and the delegate will be notified (see usage of startTimeCounter).
 */

public class IngenicoCardReaderDetector implements SearchListener {

    /** Timeout the search after 6.5 seconds  */
    private static final long TIMEOUT_DEVICE_SEARCH_MS = 6500;
    private static final long TIMEOUT_ROAM_SEARCH_MS = 6000;

    private DeviceManager rp350xRoamDeviceManager = null;
    private DeviceManager moby3000RoamDeviceManager = null;
    private MockRoamDeviceManager mockRoamDeviceManager = null;
    private List<DeviceManager> supportedDeviceManagers;
    private List<Device> discoveredDevices;

    private Config config = null;
    private CardReaderDetectionDelegate delegate = null;

    private Handler deviceDetectionTimeoutHandler = new Handler();

    private int completedDiscoveries = 0;

    public IngenicoCardReaderDetector() {
        this.rp350xRoamDeviceManager = RoamReaderUnifiedAPI.getDeviceManager(DeviceType.RP350x);
        this.moby3000RoamDeviceManager = RoamReaderUnifiedAPI.getDeviceManager(DeviceType.MOBY3000);
        this.mockRoamDeviceManager = MockRoamDeviceManager.getDeviceManager();

        this.discoveredDevices = new ArrayList<>();
        this.supportedDeviceManagers = new ArrayList<>();
    }

    public void findAvailableCardReaders(Config config, CardReaderDetectionDelegate detectionDelegate) {
        Log.d("wepay_sdk", "findAvailableCardReaders");
        MockConfig mockConfig = config.getMockConfig();

        this.config = config;
        this.delegate = detectionDelegate;

        if (mockConfig != null && mockConfig.isUseMockCardReader()) {
            this.mockRoamDeviceManager.setMockConfig(mockConfig);
            this.supportedDeviceManagers.add(this.mockRoamDeviceManager);
        } else {
            this.supportedDeviceManagers.add(this.rp350xRoamDeviceManager);
            this.supportedDeviceManagers.add(this.moby3000RoamDeviceManager);
        }

        this.beginDetection();
    }

    public void stopFindingCardReaders() {
        Log.d("wepay_sdk", "stopFindingCardReaders");
        this.stopTimeCounter(this.deviceDetectionTimeoutHandler);
        this.cancelAllDeviceManagerSearches(this.supportedDeviceManagers);
        completedDiscoveries = 0;
        this.discoveredDevices.clear();
    }

    private void beginDetection() {
        completedDiscoveries = 0;

        for (DeviceManager manager : this.supportedDeviceManagers) {
            manager.searchDevices(this.config.getContext(), false, TIMEOUT_ROAM_SEARCH_MS, this);
        }

        stopTimeCounter(this.deviceDetectionTimeoutHandler);
        startTimeCounter(this.deviceDetectionTimeoutHandler, TIMEOUT_DEVICE_SEARCH_MS);
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
                discoveryComplete();
            }
        };

        counterHandler.postDelayed(counterRunnable, timeLength);
    }

    private void stopTimeCounter(Handler counterHandler) {
        counterHandler.removeCallbacksAndMessages(null);
    }

    private void discoveryComplete() {
        Log.d("wepay_sdk", "discoveryComplete");
        this.stopTimeCounter(this.deviceDetectionTimeoutHandler);
        this.cancelAllDeviceManagerSearches(this.supportedDeviceManagers);

        if (this.discoveredDevices.size() > 0) {
            // Make a copy to give to the delegate so we can clear the list safely
            List<Device> list = new ArrayList<>(this.discoveredDevices);
            this.delegate.onCardReaderDevicesDetected(list);
            this.discoveredDevices.clear();
        }
        else {
            this.beginDetection();
        }
    }

    /**
     * Returns whether or not something is plugged into the Audio Jack of the device. If
     * provided a mock config using a mock card reader, returns isMockCardReaderDetected().
     *
     * This uses a deprecated AudioManager function `isWiredHeadsetOn()`. If the function is
     * not available, we catch the exception and just return true.
     *
     * @param cfg the Config to use
     * @return true if audio jack plugged in or mocked, false otherwise
     */
    @SuppressWarnings("deprecation")
    private boolean isAudioJackPluggedIn(Config cfg) {
        if (cfg != null
                && cfg.getMockConfig() != null
                && cfg.getMockConfig().isUseMockCardReader()) {
            return cfg.getMockConfig().isMockCardReaderDetected();
        }

        try {
            AudioManager manager = ((AudioManager)(cfg.getContext().getSystemService(Context.AUDIO_SERVICE)));
            return manager.isWiredHeadsetOn();
        } catch (Exception e) {
            return true;
        }
    }

    /** SearchListener */

    @Override
    public void onDeviceDiscovered(Device device) {
        Log.d("wepay_sdk", "onDeviceDiscovered " + device.getName());

        // Maintain a list of all discovered devices that have the name AUDIOJACK or MOB30*
        String name = device.getName();
        Boolean isMoby = name != null && name.startsWith("MOB30");
        Boolean isAudioJack = name != null && (name.equals("AUDIOJACK") || name.startsWith("RP350"));
        String rememberedName = SharedPreferencesHelper.getRememberedCardReader(this.config.getContext());

        // In Android only, we need to do a manual check that the audio jack is plugged in.
        // Roam "discovers" RP350x even when nothing is plugged in. We don't want to surface that
        // unless something is actually plugged in.
        if (isMoby || (isAudioJack && isAudioJackPluggedIn(this.config))) {
            this.discoveredDevices.add(device);

            if (name.equals(rememberedName)) {
                // Stop searching for a device if we've found the card reader we remember.
                Log.d("wepay_sdk", "onDeviceDiscovered: discovered remembered reader " + name);

                this.discoveryComplete();
            }
        }
    }

    @Override
    public void onDiscoveryComplete() {
        completedDiscoveries++;
        Log.d("wepay_sdk", "onDiscoveryComplete [" + completedDiscoveries + "]");

        if (completedDiscoveries >= this.supportedDeviceManagers.size()) {
            this.discoveryComplete();
        }
    }

    public interface CardReaderDetectionDelegate {
        void onCardReaderDevicesDetected(List<Device> devices);
    }
}
