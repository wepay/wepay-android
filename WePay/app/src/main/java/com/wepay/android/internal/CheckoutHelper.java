package com.wepay.android.internal;

import android.graphics.Bitmap;
import android.util.Base64;

import com.wepay.android.CheckoutHandler;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;

import org.apache.http.Header;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chaitanya.bagaria on 1/10/16.
 */
public class CheckoutHelper {

    /** The Constant SIGNATURE_MIN_HEIGHT. */
    private static final int SIGNATURE_MIN_HEIGHT = 64;

    /** The Constant SIGNATURE_MIN_WIDTH. */
    private static final int SIGNATURE_MIN_WIDTH = 64;

    /** The Constant SIGNATURE_MAX_HEIGHT. */
    private static final int SIGNATURE_MAX_HEIGHT = 256;

    /** The Constant SIGNATURE_MAX_WIDTH. */
    private static final int SIGNATURE_MAX_WIDTH = 256;

    /** The config. */
    private Config config;

    public CheckoutHelper(Config config) {
        this.config = config;
    }

    /**
     * Use this method to store a signature image associated with a checkout id on WePay's servers.
     * The signature can be retrieved via a server-to-server call that fetches the checkout object.
     *
     * @param image the signature image to be stored.
     * @param checkoutId the checkout id associated with the signature
     * @param checkoutHandler the signature handler
     */
    public void storeSignatureImage(final Bitmap image, final String checkoutId, final CheckoutHandler checkoutHandler) {
        Boolean isImageValid = false;
        Bitmap scaledImage = null;
        String encodedImage = null;
        Exception innerException = null;

        // validate image
        try
        {
            if(this.canStoreSignatureImage(image)) {
                // scale image
                scaledImage = this.scaledSignatureImageCopy(image);

                if (scaledImage !=null) {
                    // convert image to base64
                    encodedImage = this.storableStringForImage(scaledImage);

                    // mark image as valid
                    if (encodedImage != null) {
                        isImageValid = true;
                    }
                }
            }
        } catch (Exception e) {
            innerException = e;
        }

        // take action based on validity
        if (isImageValid) {
            // make the param map
            Map<String, Object> paramMap = getSignatureParamMap(encodedImage, checkoutId);

            // make the client call
            WepayClient.checkoutSignatureCreate(this.config, paramMap, new WepayClient.ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, JSONObject response) {
                    // extract signature url
                    String signatureUrl = response.isNull("signature_url") ? null : response.optString("signature_url");

                    // return response
                    if (checkoutHandler != null) {
                        checkoutHandler.onSuccess(signatureUrl, checkoutId);
                    }
                }

                @Override
                public void onFailure(int statusCode, Throwable throwable, JSONObject errorResponse) {
                    com.wepay.android.models.Error error;

                    if (errorResponse != null) {
                        error = new Error(errorResponse, throwable);
                    } else {
                        error = Error.getNoDataReturnedError();
                    }

                    if (checkoutHandler != null) {
                        checkoutHandler.onError(image, checkoutId, error);
                    }
                }
            });
        } else {
            // return error
            if (checkoutHandler != null) {
                checkoutHandler.onError(image, checkoutId, Error.getInvalidSignatureImageError(innerException));
            }
        }
    }

    /**
     * Checks if the given signature image can be stored
     *
     * @param image the signature image
     * @return true if image can be stored, otherwise false
     */
    private Boolean canStoreSignatureImage(Bitmap image) {
        // Check image exists
        if (image == null) {
            return false;
        }

        // get height and width
        int h = image.getHeight();
        int w = image.getWidth();

        // check trivial height width
        if (h == 0 || w == 0) {
            return false;
        }

        // if height has to be scaled up, resulting width should be acceptable
        if (h < SIGNATURE_MIN_HEIGHT && (w * SIGNATURE_MIN_HEIGHT / h > SIGNATURE_MAX_WIDTH)) {
            return false;
        }

        // if width has to be scaled up, resulting height should be acceptable
        if (w < SIGNATURE_MIN_WIDTH && (h * SIGNATURE_MIN_WIDTH / w > SIGNATURE_MAX_HEIGHT)) {
            return false;
        }

        // if height has to be scaled down, resulting width should be acceptable
        if (h > SIGNATURE_MAX_HEIGHT && (w * SIGNATURE_MAX_HEIGHT / h < SIGNATURE_MIN_WIDTH)) {
            return false;
        }

        // if width has to be scaled down, resulting height should be acceptable
        if (w > SIGNATURE_MAX_WIDTH && (h * SIGNATURE_MAX_WIDTH / w < SIGNATURE_MIN_HEIGHT)) {
            return false;
        }

        return true;
    }

    /**
     * Creates a scaled signature image copy for storage.
     *
     * @param image the image
     * @return the scaled image copy
     */
    private Bitmap scaledSignatureImageCopy(Bitmap image) {
        // default scale
        float scale = 1;

        // get height and width
        int h = image.getHeight();
        int w = image.getWidth();

        // scaling up
        if (h < SIGNATURE_MIN_HEIGHT || w < SIGNATURE_MIN_WIDTH) {
            scale = Math.max(SIGNATURE_MIN_HEIGHT / h, SIGNATURE_MIN_WIDTH / w);
        }

        // scaling down
        if (h > SIGNATURE_MAX_HEIGHT || w > SIGNATURE_MAX_WIDTH) {
            scale = Math.min(SIGNATURE_MAX_HEIGHT * 1.0f / h, SIGNATURE_MAX_WIDTH  * 1.0f / w);
        }

        int newH = (int)(h * scale);
        int newW = (int)(w * scale);

        Bitmap scaledImage = Bitmap.createScaledBitmap(image, newW, newH, true);

        return scaledImage;
    }

    /**
     * Gets the storable base-64 string representation of the image.
     *
     * @param image the image
     * @return the base-64 string representation
     */
    private String storableStringForImage(Bitmap image) {

        // convert to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 95, baos); //bm is the bitmap object
        byte[] byteArrayImage = baos.toByteArray();

        String encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);

        return encodedImage;
    }

    /**
     * Gets the signature param map.
     *
     * @param encodedImage the base-64 encoded image data
     * @param checkoutId the checkout id to associate with the signature
     * @return the signature param map
     */
    private Map<String, Object> getSignatureParamMap(String encodedImage, String checkoutId) {
        Map<String, Object> params = new HashMap<String, Object>();

        params.put("checkout_id", checkoutId);
        params.put("base64_img_data", encodedImage);

        return params;
    }

}
