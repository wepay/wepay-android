package com.wepay.android;

import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.CurrencyCode;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;

import java.math.BigDecimal;
import java.util.ArrayList;

/** \interface CardReaderHandler
 * The Interface CardReaderHandler defines the methods used to communicate information regarding the card reader.
 */
public interface CardReaderHandler {

    /**
     * Called when the EMV card contains more than one application. The applications should be presented to the payer for selection. Once the payer makes a choice, the app must execute callback.useApplicationAtIndex() with the index of the selected application. The transaction cannot proceed until the callback is executed.
     *
     * Example:
     *     callback.useApplicationAtIndex(0);
     *
     * @param callback the callback object.
     * @param applications the array of String containing application names from the card.
     */
    public void onEMVApplicationSelectionRequested(ApplicationSelectionCallback callback, ArrayList<String> applications);

    /**
     * Gets called when the card reader reads a card's information successfully.
     *
     * @param paymentInfo the payment info read from a card.
     */
    public void onSuccess(PaymentInfo paymentInfo);

    /**
     * Gets called when the card reader fails to read a card's information.
     *
     * @param error the error due to which card reading failed.
     */
    public void onError(Error error);

    /**
     * Gets called whenever the card reader changes status.
     *
     * @param status the status.
     */
    public void onStatusChange(CardReaderStatus status);

    /**
     * Gets called when the connected card reader is previously configured, to give the app an opportunity to reset the device. The app must respond by executing callback.resetCardReader(). The transaction cannot proceed until this callback is executed. The card reader must be reset here if the merchant manually resets the reader via the hardware reset button on the reader.
     * @param callback the callback object.
     */
    public void onReaderResetRequested(CardReaderResetCallback callback);

    /**
     * Gets called so that the app can provide the amount, currency code and the WePay account Id of the merchant. The app must respond by executing callback.useTransactionInfo(). The transaction cannot proceed until this callback is executed.
     * @param callback the callback object.
     */
    public void onTransactionInfoRequested(CardReaderTransactionInfoCallback callback);

    /**
     * Gets called so that an email address can be provided before a transaction is authorized. The app must respond by executing callback.insertPayerEmail(). The transaction cannot proceed until the callback is executed.
     *
     * @param callback the callback object.
     */
    public void onPayerEmailRequested(CardReaderEmailCallback callback);

    /**
     * Gets called when card reader devices have been discovered, to give the app an opportunity to select which card reader to initialize. The app must respond by executing callback. The card reader will not be initialized until the callback is executed.
     *
     * @param callback the callback object.
     * @param cardReaderNames the list of device names.
     */
    public void onCardReaderSelection(CardReaderSelectionCallback callback, ArrayList<String> cardReaderNames);

    /** \interface ApplicationSelectionCallback
     * The Interface ApplicationSelectionCallback defines the callback method used to provide information to the card reader during a Dip transaction.
     */
    public interface ApplicationSelectionCallback {
        /**
         * The callback function that must be executed by the app when onEMVApplicationSelectionRequested() is called by the SDK.
         *
         * Examples:
         *     callback.useApplicationAtIndex(0);
         *
         * @param selectedIndex the index of the selected application in the array of applications from the card.
         */
        public void useApplicationAtIndex(int selectedIndex);
    }

    /** \interface CardReaderResetCallback
     * The Interface CardReaderResetCallback defines the method used to provide information to the card reader before a transaction.
     */
    public interface CardReaderResetCallback {

        /**
         * The callback function that must be executed by the app when onReaderResetRequested() is called by the SDK.
         * <p>
         * Examples:
         * callback.resetCardReader(true);
         * callback.resetCardReader(false);
         *
         * @param shouldReset The answer to the question: "Should the card reader be reset?".
         */
        public void resetCardReader(boolean shouldReset);
    }

    /** \interface CardReaderTransactionInfoCallback
     * The Interface CardReaderTransactionInfoCallback defines the method used to provide transaction information to the card reader before a transaction.
     */
    public interface CardReaderTransactionInfoCallback {

        /**
         * The callback function that must be executed when onTransactionInfoRequested() is called by the SDK.
         * Note: In the staging environment, use amounts of 20.61, 120.61, 23.61 and 123.61 to simulate authorization errors. Amounts of 21.61, 121.61, 22.61, 122.61, 24.61, 124.61, 25.61 and 125.61 will simulate successful auth.
         * <p>
         * Example:
         * callback.useTransactionInfo(new BigDecimal("21.61"), CurrencyCode.USD, 1234567);
         *
         * @param amount       the amount for the transaction. For USD amounts, there can be a maximum of two places after the decimal point.
         * @param currencyCode the currency code for the transaction. e.g. CurrencyCode.USD.
         * @param accountId    the WePay account id of the merchant.
         */
        public void useTransactionInfo(BigDecimal amount, CurrencyCode currencyCode, long accountId);
    }

    /** \interface CardReaderEmailCallback
     * The Interface CardReaderEmailCallback defines the method used to provide email information to the card reader after a transaction.
     */
    public interface CardReaderEmailCallback {

        /**
         * The callback function that must be executed by the app when onPayerEmailRequested() is called by the SDK.
         *
         * Examples:
         *     callback.insertPayerEmail("android-example@wepay.com");
         *     callback.insertPayerEmail(null);
         *
         * @param email the payer's email address.
         */
        public void insertPayerEmail(String email);

    }

    /** \interface CardReaderSelectionCallback
     * The Interface CardReaderSelectionCallback defines the callback method used to select which card reader to initialize.
     */
    public interface CardReaderSelectionCallback {
        /**
         * The callback function that must be executed by the app when onCardReaderSelection() is called by the SDK.
         *
         * Examples:
         *     callback.useCardReaderAtIndex(0);
         *
         * @param selectedIndex the index of the selected card reader in the array of detected card readers.
         */
        public void useCardReaderAtIndex(int selectedIndex);
    }
}
