package com.wepay.android;

import com.wepay.android.models.AuthorizationInfo;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;

/** \interface AuthorizationHandler
 * The Interface AuthorizationHandler defines the method used to return data in response to an authorization call.
 */
public interface AuthorizationHandler {
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
}
