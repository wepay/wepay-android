package com.wepay.android.internal.CardReader.Utilities;

import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.wepay.android.CardReaderHandler;
import com.wepay.android.TokenizationHandler;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.internal.CardReader.DeviceHelpers.ExternalCardReaderHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.RoamHelper;
import com.wepay.android.internal.CardReaderDirector;
import com.wepay.android.internal.RiskHelper;
import com.wepay.android.internal.WepayClient;
import com.wepay.android.internal.WepayClient.HttpResponseHandler;
import com.wepay.android.internal.WepayClientHelper;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;
import java.math.BigDecimal;
import java.util.Map;
import org.json.JSONObject;

/**
 * Responsible for processing WePay's PaymentInfo. TransactionUtilities is the class that primarily
 * interfaces with the WePayClientHelper. In other words, most of the API calls to WePay servers
 * are invoked here.
 */

public class TransactionUtilities {

    private Config config = null;
    private ExternalCardReaderHelper externalCardReaderHelper = null;
    private RiskHelper riskHelper = null;
    private CardReaderDirector.CardReaderRequest cardReaderRequest;

    public TransactionUtilities(Config config,
                                ExternalCardReaderHelper helper) {
        this.config = config;
        this.externalCardReaderHelper = helper;
        this.riskHelper = new RiskHelper(config);
    }

    public void setCardReaderRequest(CardReaderDirector.CardReaderRequest requestType) {
        this.cardReaderRequest = requestType;
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
                if (cardReaderRequest == CardReaderDirector.CardReaderRequest.CARD_READER_FOR_TOKENIZING) {
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
                            Map<String, Object> paramMap = WepayClientHelper.getCreditCardParams(paymentInfo, riskHelper.getSessionId(), model, amount, currencyCode, accountId, fallback);
                            WepayClient.creditCardCreateEMV(config, paramMap, new HttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, JSONObject response) {
                                    responseHandler.onSuccess(response);
                                }

                                @Override
                                public void onFailure(int statusCode, JSONObject errorResponse, Throwable throwable) {
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

        WepayClient.creditCardAuthReverse(this.config, paramMap, new HttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, JSONObject response) {
                // do nothing
            }

            @Override
            public void onFailure(int statusCode, JSONObject errorResponse, Throwable throwable) {
                // do nothing
            }
        });
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
    private void tokenizeSwipedPaymentInfo(final PaymentInfo paymentInfo, TokenizationHandler tokenizationHandler, String model, BigDecimal amount, String currencyCode, long accountId, boolean fallback, final TransactionResponseHandler responseHandler) {
        this.externalCardReaderHelper.setTokenizationHandler(tokenizationHandler);

        Error error = this.validatePaymentInfoForTokenization(paymentInfo);
        if (error != null) {
            // invalid payment info, return error
            this.externalCardReaderHelper.informExternalCardReaderError(paymentInfo, error);
            responseHandler.onFailure(error);
        } else {
            // tokenize
            Map<String, Object> paramMap = WepayClientHelper.getCreditCardParams(paymentInfo, riskHelper.getSessionId(), model, amount, currencyCode, accountId, fallback);

            WepayClient.creditCardCreateSwipe(this.config, paramMap, new HttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, JSONObject response) {
                    String tokenId = response.isNull("credit_card_id") ? null : response.optString("credit_card_id");
                    PaymentToken token = new PaymentToken(tokenId);

                    externalCardReaderHelper.informExternalCardReaderTokenizationSuccess(paymentInfo, token);
                    responseHandler.onSuccess(response);
                }

                @Override
                public void onFailure(int statusCode, JSONObject errorResponse, Throwable throwable) {
                    Error error = (errorResponse == null) ? Error.getNoDataReturnedError() : new Error(errorResponse, throwable);
                    externalCardReaderHelper.informExternalCardReaderError(paymentInfo, error);
                    responseHandler.onFailure(error);
                }
            });
        }
    }

    public interface TransactionResponseHandler {
        void onSuccess(JSONObject response);
        void onFailure(Error error);
        void onFinish();
    }
}
