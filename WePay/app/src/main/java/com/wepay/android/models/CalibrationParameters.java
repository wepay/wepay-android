package com.wepay.android.models;

import com.google.gson.Gson;

import java.util.LinkedHashMap;

/**
 * The Class CalibrationParameters contains the parameters used to calibrate the card reader.
 */
public class CalibrationParameters {
    protected String deviceName;
    protected int wave;
    protected short sendBaud;
    protected float sendVolume;
    protected short recvBaud;
    protected short voltage;
    protected int audioSource;
    protected short frameLength;
    protected int playSampleFrequency;
    protected int recordSampleFrequency;

    public CalibrationParameters(String deviceName, int wave, short sendBaudRate, float volume, short recvBaudRate, short voltage, int audioSource, short frameLength, int playSampleFrequency, int recordSampleFrequency) {
        this.deviceName = deviceName;
        this.wave = wave;
        this.sendBaud = sendBaudRate;
        this.sendVolume = volume;
        this.recvBaud = recvBaudRate;
        this.audioSource = audioSource;
        this.voltage = voltage;
        this.frameLength = frameLength;
        this.playSampleFrequency = playSampleFrequency;
        this.recordSampleFrequency = recordSampleFrequency;
    }

    public String toString() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("deviceName", this.deviceName);
        params.put("wave", this.wave);
        params.put("sendBaud", this.sendBaud);
        params.put("sendVolume", this.sendVolume);
        params.put("recvBaud", this.recvBaud);
        params.put("audioSource", this.audioSource);
        params.put("voltage", this.voltage);
        params.put("frameLength", this.frameLength);
        params.put("playSampleFrequency", this.playSampleFrequency);
        params.put("recordSampleFrequency", this.recordSampleFrequency);

        return new Gson().toJson(params, LinkedHashMap.class);
    }
}
