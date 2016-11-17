package com.wepay.android.internal;

import android.util.Log;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.roam.roamreaderunifiedapi.DeviceManager;
import com.roam.roamreaderunifiedapi.RoamReaderUnifiedAPI;
import com.roam.roamreaderunifiedapi.callback.DeviceStatusHandler;
import com.roam.roamreaderunifiedapi.constants.DeviceType;
import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.roam.roamreaderunifiedapi.data.CalibrationParameters;
import com.wepay.android.AuthorizationHandler;
import com.wepay.android.BatteryLevelHandler;
import com.wepay.android.CalibrationHandler;
import com.wepay.android.CardReaderHandler;
import com.wepay.android.TokenizationHandler;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.internal.CardReader.DeviceHelpers.BatteryHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.CalibrationHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.ExternalCardReaderHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.RoamHelper;
import com.wepay.android.internal.CardReader.DeviceManagers.DeviceManagerDelegate;
import com.wepay.android.internal.CardReader.DeviceManagers.RP350XManager;
import com.wepay.android.internal.mock.MockRoamDeviceManager;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.MockConfig;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

import org.apache.http.Header;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Map;

/**
 * This class is detects the type of the connected reader and initiates swipe vs emv flow
 * accordingly.
 */
public class CardReaderHelper implements DeviceStatusHandler, DeviceManagerDelegate {

    private static final int CARD_READER_TIMEOUT_INFINITE_SEC = 0;
    private static final int CARD_READER_TIMEOUT_DEFAULT_SEC = 60;

    private DeviceManager roamDeviceManager = null;
    private Config config = null;
    private RP350XManager rp350xManager = null;
    private DeviceType connectedDevice = null;

    private enum CardReaderRequest{CARD_READER_FOR_READING, CARD_READER_FOR_TOKENIZING};
    private CardReaderRequest cardReaderRequest = null;

    private ExternalCardReaderHelper externalCardReaderHelper = null;

    private CalibrationHelper calibrationHelper = new CalibrationHelper();

    private String sessionId = null;

    public CardReaderHelper(Config config) {
        this.config = config;
        this.externalCardReaderHelper = new ExternalCardReaderHelper();
    }

    public void startCardReaderForReading(CardReaderHandler cardReaderHandler) {
        this.externalCardReaderHelper.setCardReaderHandler(cardReaderHandler);
        this.cardReaderRequest = CardReaderRequest.CARD_READER_FOR_READING;

        if (this.connectedDevice != null && this.connectedDevice.equals(DeviceType.RP350x)) {
            this.rp350xManager.processCard();
        } else {
            instantiateCardReaderInstance();
        }
    }

    public void startCardReaderForTokenizing(CardReaderHandler cardReaderHandler, TokenizationHandler tokenizationHandler,
                                             AuthorizationHandler authorizationHandler, String sessionId) {
        this.externalCardReaderHelper.setAuthorizationHandler(authorizationHandler);
        this.externalCardReaderHelper.setCardReaderHandler(cardReaderHandler);
        this.externalCardReaderHelper.setTokenizationHandler(tokenizationHandler);
        this.sessionId = sessionId;

        this.cardReaderRequest = CardReaderRequest.CARD_READER_FOR_TOKENIZING;

        if (this.connectedDevice != null && this.connectedDevice.equals(DeviceType.RP350x)) {
            this.rp350xManager.processCard();
        } else {
            instantiateCardReaderInstance();
        }
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

    public void calibrateCardReader(final CalibrationHandler calibrationHandler) {
        this.calibrationHelper.calibrateCardReader(calibrationHandler, this.config);
    }

    public void getCardReaderBatteryLevel(BatteryLevelHandler batteryLevelHandler) {
        BatteryHelper bh = new BatteryHelper();
        bh.getBatteryLevel(batteryLevelHandler, this.config);
    }

    private void instantiateCardReaderInstance() {
        final DeviceStatusHandler deviceStatusHandler = this;
        final DeviceManagerDelegate delegate = this;

        MockConfig mockConfig = config.getMockConfig();
        if (mockConfig != null && mockConfig.isUseMockCardReader()) {
            roamDeviceManager = MockRoamDeviceManager.getDeviceManager();
            ((MockRoamDeviceManager) roamDeviceManager).setMockConfig(mockConfig);
        } else {
            roamDeviceManager = RoamReaderUnifiedAPI.getDeviceManager(DeviceType.RP350x);
        }

        // calibrate the roam device manager
        CalibrationParameters params = this.calibrationHelper.getCalibrationParams(this.config);
        if (params != null) {
            this.roamDeviceManager.initialize(config.getContext(), deviceStatusHandler, params);
        } else {
            this.roamDeviceManager.initialize(config.getContext(), deviceStatusHandler);
        }

        // start a new RP350x manager
        this.rp350xManager = new RP350XManager(this.config, this.roamDeviceManager, delegate, this.externalCardReaderHelper);
        this.rp350xManager.startDevice();
    }

    @Override
    public void onConnected() {
        Log.d("wepay_sdk", "CRHelper onConnected");
        if (this.roamDeviceManager == null) {
            Log.d("wepay_sdk", "roamDeviceManager was null");
            instantiateCardReaderInstance();
            return;
        }

        this.connectedDevice = this.roamDeviceManager.getType();
        if (this.connectedDevice.equals(DeviceType.RP350x)) {
            this.rp350xManager.cardReaderConnected();
        }
    }

    @Override
    public void onDisconnected() {
        Log.d("wepay_sdk", "CRHelper onDisconnected");
        if (this.connectedDevice != null && this.connectedDevice.equals(DeviceType.RP350x)) {
            this.rp350xManager.cardReaderDisconnected();
        }

        this.connectedDevice = null;
    }

    @Override
    public void onError(String message) {
        Log.d("wepay_sdk", "CRHelper onError: " + message);
        if (this.connectedDevice != null && this.connectedDevice.equals(DeviceType.RP350x)) {
            this.rp350xManager.cardReaderError(message);
        }
    }

    public void handleSwipeResponse(Map<Parameter, Object> data, final String model, final BigDecimal amount, final String currencyCode, final long accountId, final boolean fallback, TransactionResponseHandler responseHandler) {
        Error error = this.validateSwiperInfoForTokenization(data);
        if (error != null) {
            // inform handler error
            this.externalCardReaderHelper.informExternalCardReaderError(error);
            responseHandler.onFailure(error);
        } else {
            String firstName = RoamHelper.getFirstName(data);
            String lastName = RoamHelper.getLastName(data);
            String paymentDescription = RoamHelper.getPaymentDescription(data);
            paymentDescription = this.sanitizePAN(paymentDescription);

            PaymentInfo paymentInfo = new PaymentInfo(firstName,lastName, paymentDescription, PaymentMethod.SWIPE, data);
            this.handlePaymentInfo(paymentInfo, model, amount, currencyCode, accountId, fallback, responseHandler);
        }
    }

    public void handlePaymentInfo(final PaymentInfo paymentInfo, final String model, final BigDecimal amount, final String currencyCode, final long accountId, final boolean fallback, final TransactionResponseHandler responseHandler) {
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
                        tokenizeSwipedPaymentInfo(paymentInfo, externalCardReaderHelper.getTokenizationHandler(), model, amount, currencyCode, accountId, fallback, responseHandler);
                    } else {
                        // validate before authorizing
                        Error error = validatePaymentInfoForTokenization(paymentInfo);

                        if (error != null) {
                            responseHandler.onFailure(error);
                        } else {
                            // inform external
                            externalCardReaderHelper.informExternalCardReader(CardReaderStatus.AUTHORIZING);

                            // authorize
                            Map<String, Object> paramMap = WepayClientHelper.getCreditCardParams(paymentInfo, sessionId, model, amount, currencyCode, accountId, fallback);
                            WepayClient.creditCardCreateEMV(config, paramMap, new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    responseHandler.onSuccess(response);
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                    if (errorResponse != null) {
                                        responseHandler.onFailure(new Error(errorResponse, throwable));
                                    } else {
                                        responseHandler.onFailure(Error.getIssuerUnreachableError());
                                    }
                                }
                            });
                        }
                    }
                } else {
                    if (responseHandler != null) {
                        // clean up
                        responseHandler.onFinish();
                    }
                }
            }
        });
    }

    public void issueReversal(Long creditCardId, Long accountId, Map<Parameter, Object> cardInfo) {
        Map<String, Object> paramMap = WepayClientHelper.getReversalRequestParams(creditCardId, accountId, cardInfo);

        WepayClient.creditCardAuthReverse(this.config, paramMap, new JsonHttpResponseHandler() {
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
    public void tokenizeSwipedPaymentInfo(final PaymentInfo paymentInfo, TokenizationHandler tokenizationHandler, String model, BigDecimal amount, String currencyCode, long accountId, boolean fallback, final TransactionResponseHandler responseHandler) {
        this.externalCardReaderHelper.setTokenizationHandler(tokenizationHandler);

        Error error = this.validatePaymentInfoForTokenization(paymentInfo);
        if (error != null) {
            // invalid payment info, return error
            this.externalCardReaderHelper.informExternalCardReaderError(paymentInfo, error);
            responseHandler.onFailure(error);
        } else {
            // tokenize
            Map<String, Object> paramMap = WepayClientHelper.getCreditCardParams(paymentInfo, sessionId, model, amount, currencyCode, accountId, fallback);

            WepayClient.creditCardCreateSwipe(this.config, paramMap, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    String tokenId = response.isNull("credit_card_id") ? null : response.optString("credit_card_id");
                    PaymentToken token = new PaymentToken(tokenId);

                    externalCardReaderHelper.informExternalCardReaderTokenizationSuccess(paymentInfo, token);
                    responseHandler.onSuccess(response);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {

                    Error error = (errorResponse == null) ? Error.getNoDataReturnedError() : new Error(errorResponse, throwable);
                    externalCardReaderHelper.informExternalCardReaderError(paymentInfo, error);
                    responseHandler.onFailure(error);
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

    public int getCardReaderTimeout() {
        // timeout depends on config
        if (config.shouldRestartTransactionAfterOtherErrors()) {
            // never time out
            return CARD_READER_TIMEOUT_INFINITE_SEC;
        } else {
            return CARD_READER_TIMEOUT_DEFAULT_SEC;
        }
    }

    /*
     * Called by card reader managers to inform their delegate that they have stopped.
     * The delegate then forgets the manager.
     */
    public void onCardReaderStopped() {
        this.connectedDevice = null;
        this.rp350xManager = null;
    }
}
