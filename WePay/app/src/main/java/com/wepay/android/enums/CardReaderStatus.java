/*
 * 
 */
package com.wepay.android.enums;

/**
 * The Enum CardReaderStatus defines all the statuses that can be returned by the swiper.
 */
public enum CardReaderStatus {

    /** Not connected. */
    NOT_CONNECTED(0),

    /** Waiting for card. */
    WAITING_FOR_CARD(1),

    /** Tokenizing. */
    TOKENIZING(2),

    /** Stopped. */
    STOPPED(3),

    /** Connected. */
    CONNECTED(4),

    /** Swipe detected. */
    SWIPE_DETECTED(5),

    /** Check card orientation. */
    CHECK_CARD_ORIENTATION(6),

    /** Checking reader. */
    CHECKING_READER(7),

    /** Configuring reader. */
    CONFIGURING_READER(8),

    /** Should not swipe EMV card. */
    SHOULD_NOT_SWIPE_EMV_CARD(9),

    /** Chip error, swipe card. */
    CHIP_ERROR_SWIPE_CARD(10),

    /** Card dipped. */
    CARD_DIPPED(11),

    /** Authorizing. */
    AUTHORIZING(12),

    /** Swipe error, swipe again. */
    SWIPE_ERROR_SWIPE_AGAIN(13);

    /** The code. */
    private final int code;

    /** \internal
     * Instantiates a new swiper status.
     *
     * @param code the code representing the card reader status
     */
    CardReaderStatus(int code)  {
        this.code = code;
    }

    /** \internal
     * Gets the code representing the swiper status.
     *
     * @return the code
     */
    public int getSwiperStatusCode() {
        return code;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        switch(this.code) {
            case 0:
                return "NOT_CONNECTED";
            case 1:
                return "WAITING_FOR_CARD";
            case 2:
                return "TOKENIZING";
            case 3:
                return "STOPPED";
            case 4:
                return "CONNECTED";
            case 5:
                return "SWIPE_DETECTED";
            case 6:
                return "CHECK_CARD_ORIENTATION";
            case 7:
                return "CHECKING_READER";
            case 8:
                return "CONFIGURING_READER";
            case 9:
                return "SHOULD_NOT_SWIPE_EMV_CARD";
            case 10:
                return "CHIP_ERROR_SWIPE_CARD";
            case 11:
                return "CARD_DIPPED";
            case 12:
                return "AUTHORIZING";
            case 13:
                return "SWIPE_ERROR_SWIPE_AGAIN";
            default:
                return "UNDEFINED_CARD_READER_STATUS";
        }
    }
}
