package com.wepay.android.internal.CardReader.DeviceHelpers;

import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.landicorp.emv.comm.api.CommParameter;
import com.landicorp.robert.comm.setting.AudioCommParam;
import com.roam.roamreaderunifiedapi.DeviceManager;
import com.roam.roamreaderunifiedapi.RoamReaderUnifiedAPI;
import com.roam.roamreaderunifiedapi.callback.CalibrationListener;
import com.roam.roamreaderunifiedapi.constants.CalibrationResult;
import com.roam.roamreaderunifiedapi.constants.DeviceType;
import com.roam.roamreaderunifiedapi.data.CalibrationParameters;
import com.wepay.android.CalibrationHandler;
import com.wepay.android.internal.SharedPreferencesHelper;
import com.wepay.android.internal.mock.MockRoamDeviceManager;
import com.wepay.android.models.Config;
import com.wepay.android.models.MockConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class CalibrationHelper {

    private DeviceManager roamDeviceManager = null;

    public void calibrateCardReader(final CalibrationHandler calibrationHandler, final Config config) {

        if (calibrationHandler == null) {
            return;
        }

        MockConfig mockConfig = config.getMockConfig();
        if (mockConfig != null && mockConfig.isUseMockCardReader()) {
            roamDeviceManager = MockRoamDeviceManager.getDeviceManager();
            ((MockRoamDeviceManager) roamDeviceManager).setMockConfig(mockConfig);
        } else {
            // any device manager capable of performing calibration can be used
            roamDeviceManager = RoamReaderUnifiedAPI.getDeviceManager(DeviceType.RP350x);
        }

        roamDeviceManager.startCalibration(
                config.getContext(),
                new CalibrationListener() {
                    @Override
                    public void onComplete(final CalibrationResult calibrationResult, final CalibrationParameters calibrationParameters) {
                        Handler mainHandler = new Handler(config.getContext().getMainLooper());

                        Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                Log.d("wepay_sdk", "CalibrationListener.onComplete: " + calibrationResult.toString());
                                switch (calibrationResult) {
                                    case Succeeded:
                                        AudioCommParam a = calibrationParameters.getParams().getAudioCommParam();
                                        com.wepay.android.models.CalibrationParameters params = new com.wepay.android.models.CalibrationParameters(
                                                getDeviceName(config),
                                                a.XCP_getWave(),
                                                a.XCP_getSendBaud(),
                                                a.XCP_getSendVolume(),
                                                a.XCP_getRecvBaud(),
                                                a.XCP_getVoltage(),
                                                a.XCP_getAudioSource(),
                                                a.XCP_getFrameLength(),
                                                a.XCP_getPlaySampleFrequency(),
                                                a.XCP_getRecordSampleFrequency());

                                        //save calibration result
                                        Boolean saved = SharedPreferencesHelper.saveCalibration(config.getContext(), params.toString());
                                        Log.d("wepay_sdk", "saving calibration operation succeeded: " + saved.toString());
                                        //return result
                                        calibrationHandler.onComplete(com.wepay.android.enums.CalibrationResult.SUCCEEDED, params);
                                        break;
                                    case Interrupted:
                                        calibrationHandler.onComplete(com.wepay.android.enums.CalibrationResult.INTERRUPTED, null);
                                    case Failed:
                                        calibrationHandler.onComplete(com.wepay.android.enums.CalibrationResult.FAILED, null);
                                        break;
                                    case Ignored:
                                    default:
                                        // do nothing
                                }
                            }
                        };
                        mainHandler.post(myRunnable);
                    }

                    @Override
                    public void onInformation(String s) {
                        Log.d("wepay_sdk", "CalibrationListener.onInformation: " + s);
                        // Do nothing
                    }

                    @Override
                    public void onProgress(final double progress) {
                        Handler mainHandler = new Handler(config.getContext().getMainLooper());

                        Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                calibrationHandler.onProgress(progress);
                            }
                        };
                        mainHandler.post(myRunnable);
                    }
                });
    }

    public CalibrationParameters getCalibrationParams(Config config) {

        CalibrationParameters savedParams = this.fetchSavedParams(config);
        if (savedParams != null) {
            Log.d("wepay_sdk", "using saved device profile");
            return savedParams;
        }

        String name = getDeviceName(config);
        Log.d("wepay_sdk", "device name: " + name);

        if (name.equalsIgnoreCase("Samsung SM-G900P")
                || name.equalsIgnoreCase("Samsung SM-G900T")
                || name.equalsIgnoreCase("Samsung SM-G900A"))
        {
            Log.d("wepay_sdk", "using special device profile");

            int wave = 1;
            short sendBaud = 3675;
            float sendVolume = 1.0f;
            short receiveBaud = 1837;
            int audioSource = 1;
            short voltage = 1000;
            short frameLength = 512;
            int playSampleFrequency = 44100;
            int recordSampleFrequency = 44100;

            AudioCommParam acp = new AudioCommParam(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength, "");
            acp.XCP_setPlaySampleFrequency(playSampleFrequency);
            acp.XCP_setRecordSampleFrequency(recordSampleFrequency);

            CommParameter cp = new CommParameter(acp, CommParameter.CommParamType.TYPE_AUDIOJACK);

            return new CalibrationParameters(cp);
        } else if (name.equalsIgnoreCase("Samsung SM-G900W8")) {
            Log.d("wepay_sdk", "using special device profile");

            int wave = 1;
            short sendBaud = 3675;
            float sendVolume = 1.0f;
            short receiveBaud = 3675;
            int audioSource = 6;
            short voltage = 60;
            short frameLength = 512;
            int playSampleFrequency = 44100;
            int recordSampleFrequency = 44100;

            AudioCommParam acp = new AudioCommParam(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength, "");
            acp.XCP_setPlaySampleFrequency(playSampleFrequency);
            acp.XCP_setRecordSampleFrequency(recordSampleFrequency);

            CommParameter cp = new CommParameter(acp, CommParameter.CommParamType.TYPE_AUDIOJACK);

            return new CalibrationParameters(cp);
        } else if(name.equalsIgnoreCase("LGE Nexus 4")
                || name.equalsIgnoreCase("LGE Nexus 5")) {
            Log.d("wepay_sdk", "using special device profile");

            int wave = 1;
            short sendBaud = 3675;
            float sendVolume = 1.0f;
            short receiveBaud = 3675;
            int audioSource = 1;
            short voltage = 1000;
            short frameLength = 512;
            int playSampleFrequency = 44100;
            int recordSampleFrequency = 44100;

            AudioCommParam acp = new AudioCommParam(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength, "");
            acp.XCP_setPlaySampleFrequency(playSampleFrequency);
            acp.XCP_setRecordSampleFrequency(recordSampleFrequency);

            CommParameter cp = new CommParameter(acp, CommParameter.CommParamType.TYPE_AUDIOJACK);

            return new CalibrationParameters(cp);
        } else {
            Log.d("wepay_sdk", "using default device profile");
            return null;
        }
    }

    private CalibrationParameters fetchSavedParams(Config config) {
        CalibrationParameters result = null;

        String savedParams = SharedPreferencesHelper.getCalibration(config.getContext());

        if (savedParams != null) {
            Log.d("wepay_sdk", "found calibration string: " + savedParams);

            try {
                JSONObject paramsJSON = new JSONObject(savedParams);

                int wave = paramsJSON.getInt("wave");
                short sendBaud = (short) paramsJSON.getInt("sendBaud");
                float sendVolume = (float) paramsJSON.getDouble("sendVolume");
                short receiveBaud = (short) paramsJSON.getInt("recvBaud");
                int audioSource = paramsJSON.getInt("audioSource");
                short voltage = (short) paramsJSON.getInt("voltage");
                short frameLength = (short) paramsJSON.getInt("frameLength");
                int playSampleFrequency = paramsJSON.getInt("playSampleFrequency");
                int recordSampleFrequency = paramsJSON.getInt("recordSampleFrequency");

                AudioCommParam acp = new AudioCommParam(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength, "");
                acp.XCP_setPlaySampleFrequency(playSampleFrequency);
                acp.XCP_setRecordSampleFrequency(recordSampleFrequency);

                CommParameter cp = new CommParameter(acp, CommParameter.CommParamType.TYPE_AUDIOJACK);

                result = new CalibrationParameters(cp);

            } catch (JSONException e) {
                e.printStackTrace();
                // do nothing
            }
        }

        return result;
    }

    private String getDeviceName(Config config) {
        MockConfig mockConfig = config.getMockConfig();
        if (mockConfig != null) {
            return mockConfig.getMockedDeviceName();
        }

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
