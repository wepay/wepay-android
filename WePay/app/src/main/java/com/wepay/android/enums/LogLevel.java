package com.wepay.android.enums;

/**
 * The levels of messages the SDK will log. We currently support showing either no log messages or
 * all of them. The SDK log level can be set in a Config object and passed into the WePay constructor.
 * @see com.wepay.android.models.Config
 * @see com.wepay.android.WePay
 */
public enum LogLevel {
    /** No log messages. */
    NONE(0),

    /** All log messages. */
    ALL(1);

    /** The log level's code. Useful to make comparisons between log levels. */
    private final int code;

    /** \internal
     * Instantiates a new log level.
     *
     * @param code the code representing the log level.
     */
    LogLevel(int code) {
        this.code = code;
    }

    /** \internal
     * Gets the code representing the log level.
     *
     * @return the code
     */
    public int getLogLevelCode() {
        return code;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        switch (this.code) {
            case 0:
                return "NONE";
            case 1:
                return "ALL";
            default:
                return "UNDEFINED_LOG_LEVEL";
        }
    }
}
