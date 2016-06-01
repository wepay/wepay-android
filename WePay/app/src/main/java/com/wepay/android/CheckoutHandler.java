package com.wepay.android;

import android.graphics.Bitmap;

import com.wepay.android.models.Error;

/**
 * The Interface CheckoutHandler defines the methods used to return results of a storeSignature operation.
 */
public interface CheckoutHandler {

    /**
     * Gets called when a signature is successfully stored for the given checkout id.
     *
     * @param signatureUrl the url for the signature image.
     * @param checkoutId the checkout id associated with the signature.
     */
    public void onSuccess(String signatureUrl, String checkoutId);

    /**
     * Gets called when an error occurs while storing a signature.
     *
     * @param image the signature image to be stored.
     * @param checkoutId the checkout id associated with the signature.
     * @param error the error which caused the failure.
     */
    public void onError(Bitmap image, String checkoutId, Error error);

}
