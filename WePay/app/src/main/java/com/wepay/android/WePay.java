/*
 * 
 */
package com.wepay.android;

import android.graphics.Bitmap;
import android.location.Address;
import android.util.Log;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.internal.CardReaderHelper;
import com.wepay.android.internal.CheckoutHelper;
import com.wepay.android.internal.RiskHelper;
import com.wepay.android.internal.WepayClient;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

import org.apache.http.Header;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Main Class containing all public endpoints.
 */
public class WePay {

    /** The config. */
    private Config config;

    /** The card reader helper. */
    private CardReaderHelper cardReaderHelper;

    /** The checkout helper. */
    private CheckoutHelper checkoutHelper;

    /** The risk helper. */
    private RiskHelper riskHelper;

    /** The is card reader available flag. */
    private boolean isCardReaderAvailable = false;

    /**
     * Instantiates a new WePay instance.
     *
     * @param config the WePay config
     */
    public WePay(Config config) {
        this.config = config;
        this.checkoutHelper = new CheckoutHelper(config);

        // check if card reader libraries are included
        try  {
            Class.forName("com.wepay.android.internal.CardReaderHelper");
            this.isCardReaderAvailable = true;
            this.cardReaderHelper = new CardReaderHelper(config);
        } catch (final ClassNotFoundException e) {
            this.isCardReaderAvailable = false;
        } catch (final NoClassDefFoundError e) {
            this.isCardReaderAvailable = false;
        }

        // check if risk libraries are included
        try  {
            Class.forName("com.wepay.android.internal.RiskHelper");
            this.riskHelper = new RiskHelper(config);
        } catch (final ClassNotFoundException e) {
            this.riskHelper = null;
        } catch (final NoClassDefFoundError e) {
            this.riskHelper = null;
        }
    }

    /**
     * Use this method if you just want to read non-sensitive data from the card, without actually charging the card.
     * Non-sensitive info from the card will be returned via the CardReaderHandler interface.
     *
     * The reader will wait 60 seconds for a card, and then return a timout error if a card is not detected.
     * The reader will automatically stop waiting for card if:
     * - a timeout occurs
     * - a successful swipe/dip is detected
     * - an unexpected error occurs
     * - stopReader is called
     *
     * However, if a general error (errorCategory:ERROR_CATEGORY_CARD_READER, errorCode:CARD_READER_GENERAL_ERROR) occurs while reading, after a few seconds delay, the reader will automatically start waiting again for another 60 seconds. At that time, CardReaderHandler's onStatusChange() method will be called with status = WAITING_FOR_CARD, and the user can try to swipe/dip again. This behavior can be configured with com.wepay.android.models.Config.
     *
     * WARNING: When this method is called, a (normally inaudible) signal is sent to the headphone jack of the phone, where the reader is expected to be connected. If headphones are connected instead of the reader, they may emit a very loud audible tone on receiving this signal. This method should only be called when the user intends to use the reader.
     *
     * @param cardReaderHandler the card reader handler
     */
    public void startCardReaderForReading(CardReaderHandler cardReaderHandler) {
        if (this.isCardReaderAvailable) {
            this.cardReaderHelper.startCardReaderForReading(cardReaderHandler);
        } else {
            Log.e("wepay_sdk", "card reader functionality is not available");
        }
    }

    /**
     * Use this method if you want to tokenize the card info.
     * Non-sensitive info from the card will be returned via the CardReaderHandler interface.
     * The card info will be tokenized by WePay's servers, and the token will be returned via the TokenizationHandler interface.
     *
     * The reader will wait 60 seconds for a card, and then return a timout error if a card is not detected.
     * The reader will automatically stop waiting for card if:
     * - a timeout occurs
     * - a successful swipe/dip is detected
     * - an unexpected error occurs
     * - stopReader is called
     *
     * However, if a general error (errorCategory:ERROR_CATEGORY_CARD_READER, errorCode:CARD_READER_GENERAL_ERROR) occurs while reading, after a few seconds delay, the reader will automatically start waiting again for another 60 seconds. At that time, CardReaderHandler's onStatusChange() method will be called with status = WAITING_FOR_CARD, and the user can try to swipe/dip again. This behavior can be configured with com.wepay.android.models.Config.
     *
     * WARNING: When this method is called, a (normally inaudible) signal is sent to the headphone jack of the phone, where the reader is expected to be connected. If headphones are connected instead of the reader, they may emit a very loud audible tone on receiving this signal. This method should only be called when the user intends to use the reader.
     *
     * @param cardReaderHandler the card reader handler
     * @param tokenizationHandler the tokenization handler
     * @param authorizationHandler the authorization handler
     */
    public void startCardReaderForTokenizing(CardReaderHandler cardReaderHandler, TokenizationHandler tokenizationHandler, AuthorizationHandler authorizationHandler) {
        if (this.isCardReaderAvailable) {
            String sessionId = (this.riskHelper == null) ? null : this.riskHelper.getSessionId();
            this.cardReaderHelper.startCardReaderForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler, sessionId);
        } else {
            Log.e("wepay_sdk", "card reader functionality is not available");
        }
    }

    /**
     * Stops the reader. In response, CardReaderHandler's onStatusChange() method will be called with status = STOPPED.
     * Any tokenization in progress will not be stopped, and its result will be delivered to the TokenizationHandler.
     */
    public void stopCardReader() {
        if (this.isCardReaderAvailable) {
            this.cardReaderHelper.stopCardReader();
        } else {
            Log.e("wepay_sdk", "card reader functionality is not available");
        }
    }

    /**
     * Use this method to tokenize any PaymentInfo object, such as one representing credit card info obtained manually.
     * The payment info will be tokenized by WePay's servers, and the token will be returned via the TokenizationHandler interface.
     *
     * @param paymentInfo the payment info to be tokenized
     * @param tokenizationHandler the tokenization handler
     */
    public void tokenize(final PaymentInfo paymentInfo, final TokenizationHandler tokenizationHandler) {
        String sessionId = (this.riskHelper == null) ? null : this.riskHelper.getSessionId();

        if (paymentInfo.getPaymentMethod() == PaymentMethod.MANUAL) {
            Map<String, Object> paramMap = getManualParamMap(paymentInfo, sessionId);

            WepayClient.post(this.config, "credit_card/create", paramMap, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                    String tokenId = response.isNull("credit_card_id") ? null : response.optString("credit_card_id");
                    PaymentToken token = new PaymentToken(tokenId);

                    tokenizationHandler.onSuccess(paymentInfo, token);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    if (errorResponse != null) {
                        final Error error = new Error(errorResponse, throwable);
                        tokenizationHandler.onError(paymentInfo, error);
                    } else {
                        final Error error = Error.getNoDataReturnedError();
                        tokenizationHandler.onError(paymentInfo, error);
                    }
                }
            });
        } else {
            final Error error = Error.getPaymentMethodCannotBeTokenizedError();
            tokenizationHandler.onError(paymentInfo, error);
        }
    }

    /**
     * Use this method to store a signature image associated with a checkout id on WePay's servers.
     * The signature can be retrieved via a server-to-server call that fetches the checkout object.
     * The aspect ratio (width:height) of the image must be between 1:4 and 4:1.
     * If needed, the image will internally be scaled to fit inside 256x256 pixels, while maintaining the original aspect ratio.
     *
     * @param image the signature image to be stored.
     * @param checkoutId the checkout id associated with the signature
     * @param checkoutHandler the signature handler
     */
    public void storeSignatureImage(final Bitmap image, final String checkoutId, final CheckoutHandler checkoutHandler) {
        this.checkoutHelper.storeSignatureImage(image, checkoutId, checkoutHandler);
    }

    /** \internal
     * Gets the manual param map.
     *
     * @param info 		the info
     * @param sessionId the session Id from Risk Helper
     * @return the manual param map
     */
    private Map<String, Object> getManualParamMap(PaymentInfo info, String sessionId) {
        Map<String, String> address = new HashMap<String, String>();
        Address billingAddress = info.getBillingAddress();

        if (billingAddress != null) {
            String address1 = billingAddress.getAddressLine(0);
            if (address1 != null) {
                address.put("address1", address1);
            }

            String address2 = billingAddress.getAddressLine(1);
            if (address2 != null) {
                address.put("address2", address2);
            }

            String city = billingAddress.getLocality();
            if (city != null) {
                address.put("city", city);
            }

            String country = billingAddress.getCountryCode();
            if (country != null) {
                address.put("country", country);
            }

            if (country != null && country.equalsIgnoreCase("US")) {
                // For US addresses
                String state = billingAddress.getAdminArea();
                if (state != null) {
                    address.put("state", state);
                }

                String zip = billingAddress.getPostalCode();
                if (zip != null) {
                    address.put("zip", zip);
                }
            } else {
                // For non-US address
                String state = billingAddress.getAdminArea();
                if (state != null) {
                    address.put("region", state);
                }

                String zip = billingAddress.getPostalCode();
                if (zip != null) {
                    address.put("postcode", zip);
                }
            }
        }

        Map<String, Object> params = (Map<String, Object>) info.getManualInfo();
        String name = info.getFullName();

        params.put("user_name", name);
        params.put("email", info.getEmail());
        params.put("address", address);

        if (sessionId != null) {
            params.put("device_token", sessionId);
        }

        if (info.isVirtualTerminal()) {
            params.put("virtual_terminal", "mobile");
        }

        return params;
    }
}
