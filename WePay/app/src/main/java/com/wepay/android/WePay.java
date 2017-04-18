/*
 * 
 */
package com.wepay.android;

import android.graphics.Bitmap;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.internal.CardReaderDirector;
import com.wepay.android.internal.CheckoutHelper;
import com.wepay.android.internal.LogHelper;
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
    private CardReaderDirector cardReaderDirector;

    /** The checkout helper. */
    private CheckoutHelper checkoutHelper;

    /** The risk helper. */
    private RiskHelper riskHelper;

    /** The is card reader available flag. */
    private boolean isCardReaderAvailable = false;

    /**
     * The handler that dispatches core SDK operations. Because all operations are queued on this
     * handler's looper, they will be serially executed. This helps prevent nasty interrupts
     * that can mess with the internal lifecycle.
     */
    private Handler operationHandler = null;

    /**
     * Instantiates a new WePay instance.
     *
     * @param config the WePay config
     */
    public WePay(Config config) {
        this.config = config;
        this.checkoutHelper = new CheckoutHelper(config);

        // Set the global log level based on the config value.
        LogHelper.logLevel = config.getLogLevel();

        // Spin up the async operation handler.
        this.operationHandler = new Handler(Looper.getMainLooper());

        // check if card reader libraries are included
        try  {
            Class.forName("com.wepay.android.internal.CardReaderDirector");
            this.isCardReaderAvailable = true;
            this.cardReaderDirector = new CardReaderDirector(config);
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
     * WARNING: When this method is called, if the "AUDIOJACK" device is selected via the onCardReaderSelection method in the CardReaderHandler interface, a (normally inaudible) signal is sent to the headphone jack of the phone, where the reader is expected to be connected. If headphones are connected instead of the reader, they may emit a very loud audible tone on receiving this signal. This method should only be called when the user intends to use a reader.
     *
     * @param cardReaderHandler the card reader handler
     */
    public void startTransactionForReading(final CardReaderHandler cardReaderHandler) {
        this.operationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isCardReaderAvailable) {
                    cardReaderDirector.startCardReaderForReading(cardReaderHandler);
                } else {
                    Log.e("wepay_sdk", "card reader functionality is not available");
                }
            }
        });
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
     * WARNING: When this method is called, if the "AUDIOJACK" device is selected via the onCardReaderSelection method in the CardReaderHandler interface, a (normally inaudible) signal is sent to the headphone jack of the phone, where the reader is expected to be connected. If headphones are connected instead of the reader, they may emit a very loud audible tone on receiving this signal. This method should only be called when the user intends to use a reader.
     *
     * @param cardReaderHandler the card reader handler
     * @param tokenizationHandler the tokenization handler
     * @param authorizationHandler the authorization handler
     */
    public void startTransactionForTokenizing(final CardReaderHandler cardReaderHandler, final TokenizationHandler tokenizationHandler, final AuthorizationHandler authorizationHandler) {
        this.operationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isCardReaderAvailable) {
                    String sessionId = getSessionID();
                    cardReaderDirector.startCardReaderForTokenizing(cardReaderHandler, tokenizationHandler, authorizationHandler, sessionId);
                } else {
                    Log.e("wepay_sdk", "card reader functionality is not available");
                }
            }
        });
    }

    /**
     * Stops the reader. In response, CardReaderHandler's onStatusChange() method will be called with status = STOPPED. The status can only be returned if you've provided a CardReaderHandler by starting a card reader operation after the WePay object was initialized.
     * Any operation in progress may not stop, and its result will be delivered to the appropriate handler.
     */
    public void stopCardReader() {
        this.operationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isCardReaderAvailable) {
                    cardReaderDirector.stopCardReader();
                } else {
                    Log.e("wepay_sdk", "card reader functionality is not available");
                }
            }
        });
    }

    /**
     * Use this method to try calibrating a charged card reader that doesn't seem to work on a device. If successful, we will store the calibration parameters on the device, and use them to connect to the card reader in future transactions. This operation only needs to be performed once during first-time setup, and only if the card reader is not automatically detected on a device.
     *
     * @param calibrationHandler the calibration handler
     */
    public void calibrateCardReader(final CalibrationHandler calibrationHandler) {
        this.operationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isCardReaderAvailable) {
                    cardReaderDirector.calibrateCardReader(calibrationHandler);
                } else {
                    Log.e("wepay_sdk", "card reader functionality is not available");
                }
            }
        });
    }
    /**
     * Use this method to get the current battery level of the card reader. If no card reader is currently connected, this method will try to find and connect to one.
     *
     * @param cardReaderHandler the card reader handler
     * @param batteryLevelHandler the battery level handler
     */
    public void getCardReaderBatteryLevel(final CardReaderHandler cardReaderHandler, final BatteryLevelHandler batteryLevelHandler) {
        this.operationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isCardReaderAvailable) {
                    cardReaderDirector.getCardReaderBatteryLevel(cardReaderHandler, batteryLevelHandler);
                } else {
                    Log.e("wepay_sdk", "card reader functionality is not available");
                }
            }
        });
    }

    /**
     * Use this method to tokenize any PaymentInfo object, such as one representing credit card info obtained manually.
     * The payment info will be tokenized by WePay's servers, and the token will be returned via the TokenizationHandler interface.
     *
     * @param paymentInfo the payment info to be tokenized
     * @param tokenizationHandler the tokenization handler
     */
    public void tokenize(final PaymentInfo paymentInfo, final TokenizationHandler tokenizationHandler) {
        final String sessionId = getSessionID();

        this.operationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (paymentInfo.getPaymentMethod() == PaymentMethod.MANUAL) {
                    Map<String, Object> paramMap = getManualParamMap(paymentInfo, sessionId);

                    WepayClient.creditCardCreate(config, paramMap, new JsonHttpResponseHandler() {
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
        });
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
        this.operationHandler.post(new Runnable() {
            @Override
            public void run() {
                checkoutHelper.storeSignatureImage(image, checkoutId, checkoutHandler);
            }
        });
    }

    /**
     * Use this method to get the name of the most recently used card reader.
     *
     * @return the name of the card reader.
     */
    public String getRememberedCardReader() {
        return this.cardReaderDirector.getRememberedCardReader(this.config.getContext());
    }

    /**
     * Use this method to clear the name of the most recently used card reader.
     */
    public void forgetRememberedCardReader() {
        this.cardReaderDirector.forgetRememberedCardReader(this.config.getContext());
    }

    /**
     * Use this method to get the name of the most recently used card reader.
     *
     * @return the name of the card reader.
     */
    public String getRememberedCardReader() {
        return this.cardReaderDirector.getRememberedCardReader(this.config.getContext());
    }

    /**
     * Use this method to clear the name of the most recently used card reader.
     */
    public void forgetRememberedCardReader() {
        this.cardReaderDirector.forgetRememberedCardReader(this.config.getContext());
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

    private String getSessionID() {
        return (this.riskHelper == null) ? null : this.riskHelper.getSessionId();
    }
}
