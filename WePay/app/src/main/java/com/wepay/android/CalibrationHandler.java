package com.wepay.android;

import com.wepay.android.enums.CalibrationResult;
import com.wepay.android.models.CalibrationParameters;

/** \interface CalibrationHandler
 * The Interface CalibrationHandler defines the methods used to communicate information regarding the card reader calibration process.
 */

public interface CalibrationHandler {

    /**
     * Gets called when the card reader calibration makes progress.
     *
     * @param progress the completion percentage [0.0 - 1.0].
     */
    public void onProgress(double progress);

    /**
     * Gets called when the calibration process is completed.
     *
     * @param result the result of calibration.
     * @param params the calibration parameters that were detected. Will be null if the result is not CalibrationResult.SUCCESS.
     */
    public void onComplete(CalibrationResult result, CalibrationParameters params);

}
