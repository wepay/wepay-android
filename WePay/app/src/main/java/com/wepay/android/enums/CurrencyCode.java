package com.wepay.android.enums;

/**
 * The Enum CurrencyCode defines all currency codes supported by the sdk.
 */
public enum CurrencyCode {

    /** USD */
    USD(0);

    /** The code. */
    private final int code;

    /** \internal
     * Instantiates a new currency code.
     *
     * @param code the code representing the currency code
     */
    CurrencyCode(int code)  {
        this.code = code;
    }

    /** \internal
     * Gets the code representing the currency code.
     *
     * @return the code
     */
    public int getCurrencyCodeCode() {
        return code;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     * Note: the 3-character ISO 4217 currency code. e.g. "USD"
     */
    public String toString() {
        switch(this.code) {
            case 0:
                return "USD";
            default:
                return "UNDEFINED_CURRENCY_CODE";
        }
    }
}
