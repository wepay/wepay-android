/*
 * 
 */
package com.wepay.android.enums;

/**
 * The Enum PaymentMethod defines all the payment methods available in the sdk.
 */
public enum PaymentMethod {

    /** Manual. */
    MANUAL(0),

    /** Swipe. */
    SWIPE(1),

    /** Dip */
    DIP(2);

    /** The code. */
    private final int code;

    /** \internal
     * Instantiates a new payment method.
     *
     * @param code the code representing the payment method
     */
    PaymentMethod(int code)  {
        this.code = code;
    }

    /** \internal
     * Gets the code representing the payment method.
     *
     * @return the code
     */
    public int getPaymentMethodCode() {
        return code;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        switch(this.code) {
            case 0:
                return "MANUAL";
            case 1:
                return "SWIPE";
            case 2:
                return "DIP";
            default:
                return "UNDEFINED_PAYMENT_METHOD";
        }
    }
}
