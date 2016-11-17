package com.wepay.android.enums;

/**
 * The Enum CalibrationResult defines all the results that can be returned by the card reader calibration process.
 */
public enum CalibrationResult {
    /** Succeeded. */
    SUCCEEDED(0),

    /** Failed. */
    FAILED(1),

    /** Interrupted */
    INTERRUPTED(2);

    /** The code. */
    private final int code;

    /** \internal
     * Instantiates a new calibration result.
     *
     * @param code the code representing the payment method
     */
    CalibrationResult(int code)  {
        this.code = code;
    }

    /** \internal
     * Gets the code representing the calibration result.
     *
     * @return the code
     */
    public int getCalibrationResultCode() {
        return code;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        switch(this.code) {
            case 0:
                return "SUCCEEDED";
            case 1:
                return "FAILED";
            case 2:
                return "INTERRUPTED";
            default:
                return "UNDEFINED_CALIBRATION_RESULT";
        }
    }
}
