package com.wepay.android;

import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

/**
 * The Interface TokenizationHandler defines the method used to return data in response to a tokenization call.
 */
public interface TokenizationHandler {

    /**
     * Gets called when a tokenization calls succeeds.
     *
     * @param paymentInfo the payment info passed to the tokenization call.
     * @param token the token representing the payment info.
     */
    public void onSuccess(PaymentInfo paymentInfo, PaymentToken token);

    /**
     * Gets called when a tokenization call fails.
     *
     * @param paymentInfo the payment info.
     * @param error the error due to which tokenization failed.
     */
    public void onError(PaymentInfo paymentInfo, Error error);

}
