package com.wepay.android.internal.CardReader.DeviceHelpers;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.landicorp.emv.comm.api.CommParameter;
import com.landicorp.robert.comm.setting.AudioCommParam;
import com.roam.roamreaderunifiedapi.data.CalibrationParameters;
import com.wepay.android.internal.SharedPreferencesHelper;
import com.wepay.android.models.Config;
import com.wepay.android.models.MockConfig;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UnitTestCalibrationHelper {
    private static final String CLIENT_ID = "171482";
    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();
    private static final String ENVIRONMENT = Config.ENVIRONMENT_STAGE;
    private Config config = new Config(CONTEXT, CLIENT_ID, ENVIRONMENT).setMockConfig(new MockConfig());

    @Test
    public void testGetCalibrationParams1() {
        CalibrationHelper calibrationHelper = new CalibrationHelper();
        SharedPreferencesHelper.clearCalibration(CONTEXT);

        // for Samsung SM-G900P
        // default device name mocked; set in MockConfig
        int wave = 1;
        short sendBaud = 3675;
        float sendVolume = 1.0f;
        short receiveBaud = 1837;
        int audioSource = 1;
        short voltage = 1000;
        short frameLength = 512;
        CalibrationParameters expectedParams = getExpectedCalibrationParameters(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength);

        CalibrationParameters resultParams = calibrationHelper.getCalibrationParams(config);

        // CalibrationParameters doesn't override the default equals() method
        Assert.assertTrue(compareCalibrationParameters(expectedParams, resultParams));
    }

    @Test
    public void testGetCalibrationParams2() {
        CalibrationHelper calibrationHelper = new CalibrationHelper();
        SharedPreferencesHelper.clearCalibration(CONTEXT);

        // for Samsung SM-G900W8
        config.getMockConfig().setMockedDeviceName("Samsung SM-G900W8");
        int wave = 1;
        short sendBaud = 3675;
        float sendVolume = 1.0f;
        short receiveBaud = 3675;
        int audioSource = 6;
        short voltage = 60;
        short frameLength = 512;
        CalibrationParameters expectedParams = getExpectedCalibrationParameters(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength);

        CalibrationParameters resultParams = calibrationHelper.getCalibrationParams(config);

        Assert.assertTrue(compareCalibrationParameters(expectedParams, resultParams));
    }

    @Test
    public void testGetCalibrationParams3() {
        CalibrationHelper calibrationHelper = new CalibrationHelper();
        SharedPreferencesHelper.clearCalibration(CONTEXT);

        // for LGE Nexus 4
        config.getMockConfig().setMockedDeviceName("LGE Nexus 4");
        int wave = 1;
        short sendBaud = 3675;
        float sendVolume = 1.0f;
        short receiveBaud = 3675;
        int audioSource = 1;
        short voltage = 1000;
        short frameLength = 512;
        CalibrationParameters expectedParams = getExpectedCalibrationParameters(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength);

        CalibrationParameters resultParams = calibrationHelper.getCalibrationParams(config);

        Assert.assertTrue(compareCalibrationParameters(expectedParams, resultParams));
    }

    private CalibrationParameters getExpectedCalibrationParameters(int wave, short sendBaud, float sendVolume, short receiveBaud, short voltage, int audioSource, short frameLength) {
        int playSampleFrequency = 44100;
        int recordSampleFrequency = 44100;
        AudioCommParam acp = new AudioCommParam(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength, "");
        acp.XCP_setPlaySampleFrequency(playSampleFrequency);
        acp.XCP_setRecordSampleFrequency(recordSampleFrequency);
        CommParameter cp = new CommParameter(acp, CommParameter.CommParamType.TYPE_AUDIOJACK);
        return new CalibrationParameters(cp);
    }

    private boolean compareCalibrationParameters(CalibrationParameters cp1, CalibrationParameters cp2) {
        String cp1Str = cp1.toString();
        String cp2Str = cp2.toString();
        String cp1CleanStr = cp1Str.substring(cp1Str.indexOf("{"));
        String cp2CleanStr = cp2Str.substring(cp2Str.indexOf("{"));
        return cp1CleanStr.equals(cp2CleanStr);
    }
}
