package com.wepay.android.enums;

/**
 * The Enum ErrorCode defines all error codes returned by the sdk itself. For error codes returned by the server api, visit https://www.wepay.com/developer/reference/errors
 */
public enum ErrorCode {

    /** The unknown error. */
    UNKNOWN_ERROR(10000),

    // 10001 - 10014 reserved for legacy error codes

    /** The no data returned error. */
    NO_DATA_RETURNED_ERROR(10015),

    /** The card reader general error. */
    CARD_READER_GENERAL_ERROR(10016),

    /** The card reader initialization error. */
    CARD_READER_INITIALIZATION_ERROR(10017),

    /** The card reader time out error. */
    CARD_READER_TIME_OUT_ERROR(10018),

    /** The card reader status error. */
    CARD_READER_STATUS_ERROR(10019),

    /** The invalid signature image error. */
    INVALID_SIGNATURE_IMAGE_ERROR(10020),

    /** The name not found error. */
    NAME_NOT_FOUND_ERROR(10021),

    /** The invalid card data error. */
    INVALID_CARD_DATA(10022),

    /** The card not supported error. */
    CARD_NOT_SUPPORTED(10023),

    /** The EMV transaction error. */
    EMV_TRANSACTION_ERROR(10024),

    /** The invalid application error. */
    INVALID_APPLICATION_ID(10025),

    /** The declined by card error. */
    DECLINED_BY_CARD(10026),

    /** The card blocked error. */
    CARD_BLOCKED(10027),

    /** The declined by issuer error. */
    CARD_DECLINED_BY_ISSUER(10028),

    /** The issuer unreachable error. */
    ISSUER_UNREACHABLE(10029),

    /** The invalid transaction info. */
    INVALID_TRANSACTION_INFO(10030),

    /** The transaction info not provided error. */
    TRANSACTION_INFO_NOT_PROVIDED(10031),

    /** The payment method cannot be tokenized error. */
    PAYMENT_METHOD_CANNOT_BE_TOKENIZED(10032),

    /** The failed to get battery info error. */
    FAILED_TO_GET_BATTERY_LEVEL(10033),

    /** The card reader not connected error. */
    CARD_READER_NOT_CONNECTED_ERROR(10034),

    /** The card reader model not supported error. */
    CARD_READER_MODEL_NOT_SUPPORTED(10035),

    /** The invalid transaction amount error. */
    INVALID_TRANSACTION_AMOUNT(10036),

    /** The invalid transaction currency code error. */
    INVALID_TRANSACTION_CURRENCY_CODE(10037),

    /** The invalid transaction account id error. */
    INVALID_TRANSACTION_ACCOUNT_ID(10038),

    /** The invalid card reader selection error. */
    INVALID_CARD_READER_SELECTION(10039),

    /** The card reader battery too low error. */
    CARD_READER_BATTERY_TOO_LOW(10040);

    /** The code. */
    private final int code;

    /** \internal
     * Instantiates a new error code.
     *
     * @param code the code
     */
    ErrorCode(int code)  {
        this.code = code;
    }

    /** \internal
     * Gets the code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        switch(this.code) {
            case 10000:
                return "UNKNOWN_ERROR";
            case 10015:
                return "NO_DATA_RETURNED_ERROR";
            case 10016:
                return "CARD_READER_GENERAL_ERROR";
            case 10017:
                return "CARD_READER_INITIALIZATION_ERROR";
            case 10018:
                return "CARD_READER_TIME_OUT_ERROR";
            case 10019:
                return "CARD_READER_STATUS_ERROR";
            case 10020:
                return "INVALID_SIGNATURE_IMAGE_ERROR";
            case 10021:
                return "NAME_NOT_FOUND_ERROR";
            case 10022:
                return "INVALID_CARD_DATA";
            case 10023:
                return "CARD_NOT_SUPPORTED";
            case 10024:
                return "EMV_TRANSACTION_ERROR";
            case 10025:
                return "INVALID_APPLICATION_ID";
            case 10026:
                return "DECLINED_BY_CARD";
            case 10027:
                return "CARD_BLOCKED";
            case 10028:
                return "CARD_DECLINED_BY_ISSUER";
            case 10029:
                return "ISSUER_UNREACHABLE";
            case 10030:
                return "INVALID_TRANSACTION_INFO";
            case 10031:
                return "TRANSACTION_INFO_NOT_PROVIDED";
            case 10032:
                return "PAYMENT_METHOD_CANNOT_BE_TOKENIZED";
            case 10033:
                return "FAILED_TO_GET_BATTERY_LEVEL";
            case 10034:
                return "CARD_READER_NOT_CONNECTED_ERROR";
            case 10035:
                return "CARD_READER_MODEL_NOT_SUPPORTED";
            case 10036:
                return "INVALID_TRANSACTION_AMOUNT";
            case 10037:
                return "INVALID_TRANSACTION_CURRENCY_CODE";
            case 10038:
                return "INVALID_TRANSACTION_ACCOUNT_ID";
            case 10039:
                return "INVALID_CARD_READER_SELECTION";
            case 10040:
                return "CARD_READER_BATTERY_TOO_LOW";
        default:
            return "UNDEFINED_ERROR";
        }
    }
}
