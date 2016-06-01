package com.wepay.android.internal;

import android.os.Build;
import android.util.Log;

import com.landicorp.emv.comm.api.CommParameter;
import com.landicorp.robert.comm.setting.AudioCommParam;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.roam.roamreaderunifiedapi.DeviceManager;
import com.roam.roamreaderunifiedapi.RoamReaderUnifiedAPI;
import com.roam.roamreaderunifiedapi.callback.DeviceStatusHandler;
import com.roam.roamreaderunifiedapi.constants.DeviceType;
import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.roam.roamreaderunifiedapi.data.CalibrationParameters;
import com.wepay.android.AuthorizationHandler;
import com.wepay.android.CardReaderHandler;
import com.wepay.android.TokenizationHandler;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.internal.CardReader.DeviceHelpers.ExternalCardReaderHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.RoamHelper;
import com.wepay.android.internal.CardReader.DeviceManagers.DeviceManagerDelegate;
import com.wepay.android.internal.CardReader.DeviceManagers.RP350XManager;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

import org.apache.http.Header;
import org.json.JSONObject;

import java.util.Map;

/**
 * This class is detects the type of the connected reader and initiates swipe vs emv flow
 * accordingly.
 */
public class CardReaderHelper implements DeviceStatusHandler, DeviceManagerDelegate {

    /** The Constant SWIPE_TIMEOUT_INFINITE_SEC. */
    private static final int SWIPE_TIMEOUT_INFINITE_SEC = -1;

    /** The Constant SWIPE_TIMEOUT_DEFAULT_SEC. */
    private static final int SWIPE_TIMEOUT_DEFAULT_SEC = 60;

    /** The Constant SWIPE_TIMEOUT_DEFAULT_MS. */
    public static final int SWIPE_TIMEOUT_DEFAULT_MS = SWIPE_TIMEOUT_DEFAULT_SEC * 1000;

    private DeviceManager roamDeviceManager = null;
    private Config config = null;
    private RP350XManager rp350xManager = null;
    private DeviceType connectedDevice = null;

    private enum CardReaderRequest{CARD_READER_FOR_READING, CARD_READER_FOR_TOKENIZING};
    private CardReaderRequest cardReaderRequest = null;

    private ExternalCardReaderHelper externalCardReaderHelper = null;

    private String sessionId = null;

    public CardReaderHelper(Config config) {
        this.config = config;
        this.externalCardReaderHelper = new ExternalCardReaderHelper();
    }

    public void startCardReaderForReading(CardReaderHandler cardReaderHandler) {
        this.externalCardReaderHelper.setCardReaderHandler(cardReaderHandler);
        this.cardReaderRequest = CardReaderRequest.CARD_READER_FOR_READING;
        instantiateCardReaderInstance();
    }

    public void startCardReaderForTokenizing(CardReaderHandler cardReaderHandler, TokenizationHandler tokenizationHandler,
                                             AuthorizationHandler authorizationHandler, String sessionId) {
        this.externalCardReaderHelper.setAuthorizationHandler(authorizationHandler);
        this.externalCardReaderHelper.setCardReaderHandler(cardReaderHandler);
        this.externalCardReaderHelper.setTokenizationHandler(tokenizationHandler);
        this.sessionId = sessionId;

        this.cardReaderRequest = CardReaderRequest.CARD_READER_FOR_TOKENIZING;
        instantiateCardReaderInstance();
    }

    public void stopCardReader()
    {
        if (connectedDevice == null) {
            Log.d("wepay_sdk", "CRHelper stopCardReader - no card reader connected");
            this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.STOPPED);
        } else if (connectedDevice.equals(DeviceType.RP350x)) {
            this.rp350xManager.stopDevice();
        }
    }

    private void instantiateCardReaderInstance() {
        final DeviceStatusHandler deviceStatusHandler = this;
        final DeviceManagerDelegate delegate = this;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {

            roamDeviceManager = RoamReaderUnifiedAPI.getDeviceManager(DeviceType.RP350x);

            CalibrationParameters params = getCalibrationParams();
            if (params != null) {
                roamDeviceManager.initialize(config.getContext(), deviceStatusHandler, params);
            } else {
                roamDeviceManager.initialize(config.getContext(), deviceStatusHandler);
            }

            // set command timeout depending on config
            if (config.shouldRestartCardReaderAfterOtherErrors()) {
                // never time out
                roamDeviceManager.getConfigurationManager().setCommandTimeout(SWIPE_TIMEOUT_INFINITE_SEC);
            } else {
                roamDeviceManager.getConfigurationManager().setCommandTimeout(SWIPE_TIMEOUT_DEFAULT_SEC);
            }

            rp350xManager = new RP350XManager(config, roamDeviceManager, delegate, externalCardReaderHelper);
            rp350xManager.startDevice();

//            }
//        }).start();
    }

    private CalibrationParameters getCalibrationParams() {

        String name = getDeviceName();
        Log.d("wepay_sdk", "device name: " + name);

        if (name.equalsIgnoreCase("Samsung SM-G900P")
                || name.equalsIgnoreCase("Samsung SM-G900T")
                || name.equalsIgnoreCase("Samsung SM-G900A"))
        {
            Log.d("wepay_sdk", "using special device profile");

            int wave = 1;
            short sendBaud = 3675;
            float sendVolume = 1.0f;
            short receiveBaud = 1837;
            int audioSource = 1;
            short voltage = 1000;
            short frameLength = 512;
            int playSampleFrequency = 44100;
            int recordSampleFrequency = 44100;

            AudioCommParam acp = new AudioCommParam(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength, "");
            acp.XCP_setPlaySampleFrequency(playSampleFrequency);
            acp.XCP_setRecordSampleFrequency(recordSampleFrequency);

            CommParameter cp = new CommParameter(acp, CommParameter.CommParamType.TYPE_AUDIOJACK);

            return new CalibrationParameters(cp);
        } else if (name.equalsIgnoreCase("Samsung SM-G900W8")) {
            Log.d("wepay_sdk", "using special device profile");

            int wave = 1;
            short sendBaud = 3675;
            float sendVolume = 1.0f;
            short receiveBaud = 3675;
            int audioSource = 6;
            short voltage = 60;
            short frameLength = 512;
            int playSampleFrequency = 44100;
            int recordSampleFrequency = 44100;

            AudioCommParam acp = new AudioCommParam(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength, "");
            acp.XCP_setPlaySampleFrequency(playSampleFrequency);
            acp.XCP_setRecordSampleFrequency(recordSampleFrequency);

            CommParameter cp = new CommParameter(acp, CommParameter.CommParamType.TYPE_AUDIOJACK);

            return new CalibrationParameters(cp);
        } else if(name.equalsIgnoreCase("LGE Nexus 4")
                || name.equalsIgnoreCase("LGE Nexus 5")) {
            Log.d("wepay_sdk", "using special device profile");

            int wave = 1;
            short sendBaud = 3675;
            float sendVolume = 1.0f;
            short receiveBaud = 3675;
            int audioSource = 1;
            short voltage = 1000;
            short frameLength = 512;
            int playSampleFrequency = 44100;
            int recordSampleFrequency = 44100;

            AudioCommParam acp = new AudioCommParam(wave, sendBaud, sendVolume, receiveBaud, voltage, audioSource, frameLength, "");
            acp.XCP_setPlaySampleFrequency(playSampleFrequency);
            acp.XCP_setRecordSampleFrequency(recordSampleFrequency);

            CommParameter cp = new CommParameter(acp, CommParameter.CommParamType.TYPE_AUDIOJACK);

            return new CalibrationParameters(cp);
        } else {
            Log.d("wepay_sdk", "using default device profile");
            return null;
        }
    }

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    @Override
    public void onConnected() {
        Log.d("wepay_sdk", "CRHelper onConnected");
        if (this.roamDeviceManager == null) {
            Log.v("wepay_sdk", "cardreader was null");
            instantiateCardReaderInstance();
            return;
        }

        connectedDevice = this.roamDeviceManager.getType();
        if (connectedDevice.equals(DeviceType.RP350x)) {
            rp350xManager.processCard();
            rp350xManager.cardReaderConnected();
        }
    }

    @Override
    public void onDisconnected() {
        Log.d("wepay_sdk", "CRHelper onDisconnected");
        if (connectedDevice != null && connectedDevice.equals(DeviceType.RP350x)) {
            this.rp350xManager.cardReaderDisconnected();
        }

        connectedDevice = null;
    }

    @Override
    public void onError(String message) {
        Log.d("wepay_sdk", "CRHelper onError: " + message);
        if (connectedDevice != null && connectedDevice.equals(DeviceType.RP350x)) {
            this.rp350xManager.cardReaderError(message);
        }
    }

    public void handleSwipeResponse(Map<Parameter, Object> data, final String model, final double amount, final String currencyCode, final long accountId, final boolean fallback) {
        Error error = this.validateSwiperInfoForTokenization(data);
        if (error != null) {
            // inform handler error
            this.externalCardReaderHelper.informExternalCardReaderError(error);
        } else {
            String firstName = RoamHelper.getFirstName(data);
            String lastName = RoamHelper.getLastName(data);
            String paymentDescription = RoamHelper.getPaymentDescription(data);
            paymentDescription = this.sanitizePAN(paymentDescription);

            PaymentInfo paymentInfo = new PaymentInfo(firstName,lastName, paymentDescription, PaymentMethod.SWIPE, data);
            this.handlePaymentInfo(paymentInfo, model, amount, currencyCode, accountId, fallback);
        }
    }

    public void handlePaymentInfo(final PaymentInfo paymentInfo, final String model, final double amount, final String currencyCode, final long accountId, final boolean fallback) {
        this.handlePaymentInfo(paymentInfo, model, amount, currencyCode, accountId, fallback, null);
    }

    public void handlePaymentInfo(final PaymentInfo paymentInfo, final String model, final double amount, final String currencyCode, final long accountId, final boolean fallback, final AuthResponseHandler authResponseHandler) {
        this.externalCardReaderHelper.informExternalCardReaderEmailCallback(new CardReaderHandler.CardReaderEmailCallback() {
            @Override
            public void insertPayerEmail(String email) {
                if (email != null) {
                    paymentInfo.addEmail(email);
                }

                // send paymentInfo to external delegate
                externalCardReaderHelper.informExternalCardReaderSuccess(paymentInfo);

                // tokenize if requested
                if (cardReaderRequest == CardReaderRequest.CARD_READER_FOR_TOKENIZING) {
                    if (paymentInfo.getPaymentMethod() == PaymentMethod.SWIPE) {
                        // inform external
                        externalCardReaderHelper.informExternalCardReader(CardReaderStatus.TOKENIZING);
                        // tokenize
                        tokenizeSwipedPaymentInfo(paymentInfo, externalCardReaderHelper.getTokenizationHandler(), model, amount, currencyCode, accountId, fallback);
                    } else {
                        // validate before authorizing
                        Error error = validatePaymentInfoForTokenization(paymentInfo);

                        if (error != null) {
                            authResponseHandler.onFailure(error);
                        } else {
                            // inform external
                            externalCardReaderHelper.informExternalCardReader(CardReaderStatus.AUTHORIZING);

                            // authorize
                            Map<String, Object> paramMap = WepayClientHelper.getCreditCardParams(paymentInfo, sessionId, model, amount, currencyCode, accountId, fallback);
                            WepayClient.post(config, "credit_card/create_emv", paramMap, new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    authResponseHandler.onSuccess(response);
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                    if (errorResponse != null) {
                                        authResponseHandler.onFailure(new Error(errorResponse, throwable));
                                    } else {
                                        authResponseHandler.onFailure(Error.getIssuerUnreachableError());
                                    }
                                }
                            });
                        }
                    }
                } else {
                    // do nothing, transaction is complete
                }
            }
        });
    }

    public void issueReversal(Long creditCardId, Long accountId, Map<Parameter, Object> cardInfo) {
        Map<String, Object> paramMap = WepayClientHelper.getReversalRequestParams(creditCardId, accountId, cardInfo);

        WepayClient.post(this.config, "credit_card/auth_reverse", paramMap, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // do nothing
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                // do nothing
            }
        });
    }

    /**
     * Validates a payment info instance for tokenization.
     *
     * @param paymentInfo the payment info to be validated.
     * @return Error instance if validation fails, else null.
     */
    public Error validatePaymentInfoForTokenization(PaymentInfo paymentInfo) {
        Map<Parameter, Object> swiperInfo = (Map<Parameter, Object>) paymentInfo.getCardReaderInfo();
        return this.validateSwiperInfoForTokenization(swiperInfo);
    }

    /**
     * Validates a payment info instance for tokenization.
     *
     * @param swiperInfo the swiper info to be validated.
     * @return Error instance if validation fails, else null.
     */
    public Error validateSwiperInfoForTokenization(Map<Parameter, Object> swiperInfo) {
        // if the swiper info has an error code, return the appropriate error
        if (swiperInfo.get(Parameter.ErrorCode) != null) {
            return Error.getErrorWithCardReaderResponseData(swiperInfo);
        }

        // check if name exists
        String fullName = RoamHelper.getFullName(swiperInfo);
        if (fullName == null) {
            // this indicates a bad swipe or an unsupported card.
            // we expect all supported cards to return a name
            return Error.getNameNotFoundError();
        }

        // check if encrypted track exists
        String encryptedTrack = (String) swiperInfo.get(Parameter.EncryptedTrack);
        if (encryptedTrack == null) {
            // this indicates a bad swipe or an unsupported card.
            // we expect all supported cards to return a name
            return Error.getInvalidCardDataError();
        }

        // check if ksn exists
        String ksn = (String) swiperInfo.get(Parameter.KSN);
        if (ksn == null) {
            // this indicates a bad swipe or an unsupported card.
            // we expect all supported cards to return a name
            return Error.getInvalidCardDataError();
        }

        // no issues
        return null;
    }

    /**
     * Tokenize a paymentInfo object containing swiped info.
     *
     * @param paymentInfo the swiped payment info
     * @param tokenizationHandler the tokenization handler
     * @param model the device model
     * @param amount the amount
     * @param currencyCode the currency code
     * @param accountId the account id
     * @param fallback if this is an emv fallback swipe
     */
    public void tokenizeSwipedPaymentInfo(final PaymentInfo paymentInfo, TokenizationHandler tokenizationHandler, String model, double amount, String currencyCode, long accountId, boolean fallback) {
        this.externalCardReaderHelper.setTokenizationHandler(tokenizationHandler);

        Error error = this.validatePaymentInfoForTokenization(paymentInfo);
        if (error != null) {
            // invalid payment info, return error
            this.externalCardReaderHelper.informExternalCardReaderError(paymentInfo, error);
        } else {
            // tokenize
            Map<String, Object> paramMap = WepayClientHelper.getCreditCardParams(paymentInfo, sessionId, model, amount, currencyCode, accountId, fallback);

            WepayClient.post(this.config, "credit_card/create_swipe", paramMap, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    String tokenId = response.isNull("credit_card_id") ? null : response.optString("credit_card_id");
                    PaymentToken token = new PaymentToken(tokenId);

                    externalCardReaderHelper.informExternalCardReaderTokenizationSuccess(paymentInfo, token);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    if (errorResponse != null) {
                        final Error error = new Error(errorResponse, throwable);
                        externalCardReaderHelper.informExternalCardReaderError(paymentInfo, error);
                    } else {
                        final Error error = Error.getNoDataReturnedError();
                        externalCardReaderHelper.informExternalCardReaderError(paymentInfo, error);
                    }
                }
            });
        }
    }

    public String sanitizePAN(String pan) {
        if (pan == null) {
            return pan;
        }

        String result = pan.replace("F", "");
        int length = result.length();

        if (length > 4) {
            result = new String(new char[length-4]).replace("\0", "X") + result.substring(length - 4);
        }

        return result;
    }

}
