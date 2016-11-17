package com.wepay.android;

/** \interface BatteryLevelHandler
 * The Interface BatteryLevelHandler defines the methods used to communicate information regarding the card reader's battery level.
 */
public interface BatteryLevelHandler {
    /**
     * Gets called when the card reader's battery level is determined.
     *
     * @param batteryLevel the card reader's battery charge level (0-100%).
     */
    public void onBatteryLevel(int batteryLevel);

    /**
     * Gets called when we fail to determine the card reader's battery level.
     *
     * @param error the error due to which battery level reading failed.
     */
    public void onBatteryLevelError(com.wepay.android.models.Error error);
}
