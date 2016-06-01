package com.wepay.android;

import com.wepay.android.models.AuthorizationInfo;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;

import java.util.ArrayList;

/** \interface AuthorizationHandler
 * The Interface AuthorizationHandler defines the method used to return data in response to an authorization call.
 */
public interface AuthorizationHandler {
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
     * Called when an authorization call succeeds.
     *
     * @param paymentInfo the payment info for the card that was authorized.
     * @param authorizationInfo the authorization info for the transaction that was authorized.
     */
    public void onAuthorizationSuccess(PaymentInfo paymentInfo, AuthorizationInfo authorizationInfo);

    /**
     * Called when an authorization call fails.
     *
     * @param paymentInfo the payment info for the card that failed authorization.
     * @param error the error which caused the failure.
     */
    public void onAuthorizationError(PaymentInfo paymentInfo, Error error);

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
}
