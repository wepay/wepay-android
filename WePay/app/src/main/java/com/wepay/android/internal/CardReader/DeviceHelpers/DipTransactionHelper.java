package com.wepay.android.internal.CardReader.DeviceHelpers;

import android.util.Base64;
import android.util.Log;

import com.roam.roamreaderunifiedapi.DeviceManager;
import com.roam.roamreaderunifiedapi.callback.DeviceResponseHandler;
import com.roam.roamreaderunifiedapi.constants.Command;
import com.roam.roamreaderunifiedapi.constants.ErrorCode;
import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.roam.roamreaderunifiedapi.constants.ProgressMessage;
import com.roam.roamreaderunifiedapi.constants.ResponseCode;
import com.roam.roamreaderunifiedapi.constants.ResponseType;
import com.roam.roamreaderunifiedapi.data.ApplicationIdentifier;
import com.wepay.android.AuthorizationHandler;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.internal.CardReader.DeviceManagers.DeviceManagerDelegate;
import com.wepay.android.internal.CardReader.DeviceManagers.RP350XManager;
import com.wepay.android.models.AuthorizationInfo;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Created by chaitanya.bagaria on 1/10/16.
 */
public class DipTransactionHelper implements DeviceResponseHandler {

    /** The Constant CRYPTOGRAM_INFORMATION_DATA_00. AAC (decline) */
    private static final String CRYPTOGRAM_INFORMATION_DATA_00 = "00";

    /** The Constant CRYPTOGRAM_INFORMATION_DATA_40. TC  (approve) */
    private static final String CRYPTOGRAM_INFORMATION_DATA_40 = "40";

    /** The Constant CRYPTOGRAM_INFORMATION_DATA_80. ARQC (online) */
    public static final String CRYPTOGRAM_INFORMATION_DATA_80 = "80";

    /** The Constant AUTH_RESPONSE_CODE_ONLINE_APPROVE. Any other code is decline */
    public static final String AUTH_RESPONSE_CODE_ONLINE_APPROVE = "00";

    /** The Constant MAGIC_TC. */
    public static final String MAGIC_TC = "0123456789ABCDEF";


    private long accountId;
    private double amount;
    private String currencyCode;

    /** The config. */
    private Config config = null;

    /** The roam device manager */
    private DeviceManager roamDeviceManager = null;

    /** The device manager delegate **/
    private DeviceManagerDelegate deviceManagerDelegate = null;

    /** The external card reader helper. */
    private ExternalCardReaderHelper externalCardReaderHelper = null;

    /** The dip config helper. */
    private DipConfigHelper dipConfighelper = null;

    /** TheRP350X manager. */
    private RP350XManager delegate = null;

    private PaymentInfo paymentInfo = null;

    private String selectedAID;
    private String applicationCryptogram;
    private String issuerAuthenticationData;
    private String authResponseCode;
    private String authCode;
    private String issuerScriptTemplate1;
    private String issuerScriptTemplate2;
    private String creditCardId;

    private boolean shouldReportSwipedEMVCard;
    private boolean shouldReportCheckCardOrientation;
    private boolean isFallbackSwipe;
    private boolean shouldIssueReversal;

    private Error authorizationError;
    private Runnable postStopRunnable = null;

    public DipTransactionHelper(Config config, RP350XManager rp350xManager, DeviceManagerDelegate deviceManagerDelegate, DipConfigHelper dipConfighelper) {
        this.config = config;
        this.deviceManagerDelegate = deviceManagerDelegate;
        this.dipConfighelper = dipConfighelper;
        this.delegate = rp350xManager;
    }

    public void performEMVTransactionStartCommand(double amount, String currencyCode, long accountId, DeviceManager roamDeviceManager,  ExternalCardReaderHelper externalCardReaderHelper) {
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.accountId = accountId;
        this.roamDeviceManager = roamDeviceManager;
        this.externalCardReaderHelper = externalCardReaderHelper;

        this.startTransaction();
    }

    private void startTransaction() {
        this.shouldReportSwipedEMVCard = false;
        this.shouldReportCheckCardOrientation = true;
        this.isFallbackSwipe = false;
        this.shouldIssueReversal = false;

        this.selectedAID = null;
        this.authCode = null;
        this.issuerAuthenticationData = null;
        this.authResponseCode = null;
        this.applicationCryptogram = null;
        this.issuerScriptTemplate1 = null;
        this.issuerScriptTemplate2 = null;
        this.creditCardId = null;
        this.authorizationError = null;

        this.executeCommand(Command.EMVStartTransaction, this);
    }

    private Map<Parameter, Object> getStartTransactionInputMap() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd");
        EnumMap<Parameter, Object> input = new EnumMap<Parameter, Object>(
                Parameter.class);

        // // // // // // //
        // Required params:
        // // // // // // //

        input.put(Parameter.Command, Command.EMVStartTransaction);
        input.put(Parameter.TransactionCurrencyCode, this.convertToEmvCurrencyCode(this.currencyCode));
        input.put(Parameter.TransactionType, "00");
        input.put(Parameter.TransactionDate, dateFormat.format(new Date()));
        input.put(Parameter.TerminalCapabilities, "E028C8");
        input.put(Parameter.TerminalType, "22");
        input.put(Parameter.AdditionalTerminalCapabilities, "6000008001");
        input.put(Parameter.DefaultValueForDDOL, "9F3704");
        input.put(Parameter.AuthorizationResponseCodeList,
                "59315A3159325A3259335A333030303530313034");

        // // // // // // //
        // Optional Params:
        // // // // // // //
        input.put(Parameter.TerminalCountryCode, "0840");
        input.put(Parameter.AmountAuthorizedNumeric, this.convertToEMVAmount(this.amount));
        input.put(Parameter.AmountOtherNumeric, "000000000000");

        Log.d("wepay_sdk", "getStartTransactionInputMap:\n" + input.toString());
        return input;
    }

    private Map<Parameter, Object> getFinalApplicationSelectionInputMap(ApplicationIdentifier applicationIdentifier) {
        EnumMap<Parameter, Object> input = new EnumMap<Parameter, Object>(Parameter.class);
        input.put(Parameter.Command, Command.EMVFinalApplicationSelection);
        if (applicationIdentifier != null) {
            input.put(Parameter.ApplicationIdentifier, applicationIdentifier.getAID());
        }

        Log.d("wepay_sdk", "getFinalApplicationSelectionInputMap:\n" + input.toString());
        return input;
    }

    private Map<Parameter, Object> getTransactionDataInputMap() {
        EnumMap<Parameter, Object> input = new EnumMap<Parameter, Object>(
                Parameter.class);

        // // // // // // //
        // Required params:
        // // // // // // //

        input.put(Parameter.Command, Command.EMVTransactionData);
        input.put(Parameter.TerminalFloorLimit, "00000000");
        input.put(Parameter.ThresholdValue, "00000000");
        input.put(Parameter.TargetPercentage, "00");
        input.put(Parameter.Maximumtargetpercentage, "00");

        ArrayList<String> tacList = this.dipConfighelper.getTACsForAID(this.selectedAID);

        input.put(Parameter.TerminalActionCodeDenial, tacList.get(0));
        input.put(Parameter.TerminalActionCodeOnline, tacList.get(1));
        input.put(Parameter.TerminalActionCodeDefault, tacList.get(2));

        Log.d("wepay_sdk", "getTransactionDataInputMap:\n" + input.toString());
        return input;
    }

    private Map<Parameter, Object> getCompleteTransactionInputMap() {
        EnumMap<Parameter, Object> input = new EnumMap<Parameter, Object>(Parameter.class);
        input.put(Parameter.Command, Command.EMVCompleteTransaction);

        if (this.issuerAuthenticationData == null && this.authResponseCode == null) {
            // did not go online
            input.put(Parameter.ResultofOnlineProcess, "02");
        } else {
            // went online

            // // // // // // //
            // Required params:
            // // // // // // //

            input.put(Parameter.ResultofOnlineProcess, "01");

            // this can this be absent in degraded mode?
            if (this.issuerAuthenticationData != null) {
                input.put(Parameter.IssuerAuthenticationData, this.issuerAuthenticationData);
            }

            input.put(Parameter.AuthorizationResponseCode, this.convertResponseCodeToHexString(this.authResponseCode));


            // // // // // // //
            // Optional params:
            // // // // // // //

            if (this.authCode != null) {
                input.put(Parameter.AuthorizationCode, this.authCode);
            }

            if (this.issuerScriptTemplate1 != null) {
                input.put(Parameter.IssuerScript1, this.issuerScriptTemplate1);
            }

            if (this.issuerScriptTemplate2 != null) {
                input.put(Parameter.IssuerScript2, this.issuerScriptTemplate2);
            }
        }

        Log.d("wepay_sdk", "getCompleteTransactionInputMap:\n" + input.toString());
        return input;
    }

    private Map<Parameter, Object> getTransactionStopInputMap() {
        EnumMap<Parameter, Object> input = new EnumMap<Parameter, Object>(
                Parameter.class);
        input.put(Parameter.Command, Command.EMVTransactionStop);

        Log.d("wepay_sdk", "getTransactionStopInputMap:\n" + input.toString());
        return input;
    }

    private void executeCommand(final Command cmd, final DeviceResponseHandler handler) {
        Log.d("wepay_sdk", "Executing " + cmd.toString());
        new Thread(new Runnable() {
            @Override
            public void run() {
                switch (cmd) {
                    case EMVStartTransaction:
                        roamDeviceManager.getTransactionManager().sendCommand(
                                getStartTransactionInputMap(), handler);
                        break;
                    case EMVTransactionData:
                        roamDeviceManager.getTransactionManager().sendCommand(
                                getTransactionDataInputMap(), handler);
                        break;
                    case EMVCompleteTransaction:
                        roamDeviceManager.getTransactionManager().sendCommand(
                                getCompleteTransactionInputMap(), handler);
                        break;
                    case EMVTransactionStop:
                        if (roamDeviceManager != null) {
                            roamDeviceManager.getTransactionManager().sendCommand(
                                    getTransactionStopInputMap(), handler);
                        }
                        break;
                    default:
                        break;

                }
            }
        }).start();
    }

    @Override
    public void onResponse(Map<Parameter, Object> data) {
        Log.d("wepay_sdk", data.toString());
        this.shouldReportCheckCardOrientation = false;

        Error error = this.validateEMVResponse(data);
        if (error != null) {
            if (this.shouldIssueReversal) {
                // shouldIssueReversal is set inside the validator
                this.deviceManagerDelegate.issueReversal(Long.parseLong(this.creditCardId), this.accountId, data);
            }

            // we found an error, react to it if we're supposed to
            if (this.shouldReactToError(error)) {
                this.reportAuthorizationSuccess(null, error, this.paymentInfo);
                this.reactToError(error);
            } else {
                // nothing to do here, the flow will continue elsewhere
            }
        } else {
            Command cmd = (Command) data.get(Parameter.Command);
            ResponseType responseType = (ResponseType) data.get(Parameter.ResponseType);
            switch (cmd) {
                case EMVStartTransaction:
                    if (responseType == ResponseType.MAGNETIC_CARD_DATA) {
                        this.deviceManagerDelegate.handleSwipeResponse(data, this.roamDeviceManager.getType().toString(), this.amount, this.currencyCode, this.accountId, this.isFallbackSwipe);
                        Error swipeError = this.deviceManagerDelegate.validateSwiperInfoForTokenization(data);
                        this.reactToError(swipeError, PaymentMethod.SWIPE);

                    } else if (responseType == ResponseType.LIST_OF_AIDS) {
                        this.performApplicationSelection((List<ApplicationIdentifier>) data.get(Parameter.ListOfApplicationIdentifiers));
                    } else {
                        this.handleStartTransactionResponse(data);
                    }
                    break;

                case EMVFinalApplicationSelection:
                    this.handleStartTransactionResponse(data);
                    break;

                case EMVTransactionData:
                    this.handleTransactionDataResponse(data);
                    break;

                case EMVCompleteTransaction:
                    this.handleCompleteTransactionResponse(data);
                    break;

                case EMVTransactionStop:
                    if (this.postStopRunnable != null) {
                        this.postStopRunnable.run();
                    }

                    this.postStopRunnable = null;
                    break;

                default:
                    break;

            }
        }
    }

    private void performApplicationSelection(final List<ApplicationIdentifier> applications) {
        final DeviceResponseHandler transactionResponseHandler = this;

        if (null != applications && applications.size() > 0) {
            ArrayList<String> applicationIdentifierList = new ArrayList<>();
            for (int i = 0; i < applications.size(); i++) {
                applicationIdentifierList.add(applications.get(i).getApplicationLabel());
            }

            this.externalCardReaderHelper.informExternalAuthorizationApplications(new AuthorizationHandler.ApplicationSelectionCallback() {
                @Override
                public void useApplicationAtIndex(int selectedIndex) {
                    if (selectedIndex < 0 || selectedIndex >= applications.size()) {
                        Error error = Error.getInvalidApplicationIdError();
                        reportAuthorizationSuccess(null, error, null);
                        reactToError(error);
                    } else {
                        selectedAID = applications.get(selectedIndex).getAID();
                        roamDeviceManager.getTransactionManager().sendCommand(getFinalApplicationSelectionInputMap(applications.get(selectedIndex)), transactionResponseHandler);
                    }
                }
            }, applicationIdentifierList);
        } else {
            Error error = Error.getCardReaderGeneralErrorWithMessage("Invalid data received from card");
            reportAuthorizationSuccess(null, error, null);
            reactToError(error);
        }
    }

    private void handleStartTransactionResponse(Map<Parameter, Object> data) {
        String selectedAID = (String) data.get(Parameter.ApplicationIdentifier);

        // selected AID can be null here if Application selection was performed.
        // In that case, selected AID has already been saved.
        if (selectedAID != null) {
            this.selectedAID = selectedAID;
        }

        this.executeCommand(Command.EMVTransactionData, this);
    }

    private void handleTransactionDataResponse(Map<Parameter, Object> data) {
        String firstName = RoamHelper.getFirstName(data);
        String lastName = RoamHelper.getLastName(data);
        String paymentDescription = (String) data.get(Parameter.PAN);
        paymentDescription = this.deviceManagerDelegate.sanitizePAN(paymentDescription);

        this.paymentInfo = new PaymentInfo(firstName, lastName, paymentDescription, PaymentMethod.DIP, data);

        // save application cryptogram for later use
        this.applicationCryptogram = (String) data.get(Parameter.ApplicationCryptogram);


        this.deviceManagerDelegate.handlePaymentInfo(paymentInfo, this.roamDeviceManager.getType().toString(), this.amount, this.currencyCode, this.accountId, false, new DeviceManagerDelegate.AuthResponseHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                String tempAuthCode = response.isNull("authorisation_code") ? null : response.optString("authorisation_code");
                if (tempAuthCode != null) {
                    authCode = String.format("%012d", Integer.parseInt(tempAuthCode));
                } else {
                    // a 12-digit auth code is required, even all-zeros works
                    authCode = String.format("%012d", Integer.parseInt("0"));
                }

                String issuerAuthenticationData = response.isNull("issuer_authentication_data") ? null : response.optString("issuer_authentication_data");
                String authResponseCode = response.isNull("authorisation_response_code") ? null : response.optString("authorisation_response_code");

                if (authResponseCode != null && authResponseCode.equalsIgnoreCase("217")) {
                    // 217 is an error code that comes back in case of a processor timeout
                    // This should be treated as a no-response
                    authResponseCode = null;
                }

                long tokenId = response.optLong("credit_card_id", 0);
                String creditCardId = null;

                if (tokenId == 0) {
                    creditCardId = null;
                } else {
                    creditCardId = String.format("%d", tokenId);
                }

                issuerScriptTemplate1 = response.isNull("issuer_script_template1") ? null : response.optString("issuer_script_template1");
                issuerScriptTemplate2 = response.isNull("issuer_script_template2") ? null : response.optString("issuer_script_template2");

                consumeAuthenticationData(issuerAuthenticationData, authResponseCode, creditCardId);
            }

            @Override
            public void onFailure(Error error) {
                authorizationError = error;
                consumeAuthenticationData(null, null, null);
            }
        });
    }

    private void handleCompleteTransactionResponse(Map<Parameter, Object> data) {
        // handle success
        String tc = (String) data.get(Parameter.ApplicationCryptogram);

        // inform external success
        this.reportAuthorizationSuccess(this.createAuthInfo(tc), null, this.paymentInfo);
        this.reactToError(null);
    }

    private void reportAuthorizationSuccess(AuthorizationInfo authInfo, Error error, PaymentInfo paymentInfo) {
        if (authInfo != null) {
            this.externalCardReaderHelper.informExternalAuthorizationSuccess(paymentInfo, authInfo);
        } else if (error != null) {
            if (paymentInfo != null) {
                this.externalCardReaderHelper.informExternalAuthorizationError(paymentInfo, error);
            } else {
                this.externalCardReaderHelper.informExternalCardReaderError(error);
            }
        }
    }

    private AuthorizationInfo createAuthInfo(String tc) {
        String text = tc + "+" + this.creditCardId;
        byte[] data = null;
        try {
            data = text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        String base64 = Base64.encodeToString(data, Base64.DEFAULT | Base64.NO_WRAP);
        return new AuthorizationInfo(this.amount, base64, this.creditCardId);
    }

    private void reactToError(Error error) {
        this.reactToError(error, PaymentMethod.DIP);
    }

    private void reactToError(final Error error, final PaymentMethod paymentMethod) {
        // stop (end) transaction, then either restart transaction or stop reader
        this.postStopRunnable = new Runnable() {
            @Override
            public void run() {
                if (delegate.shouldKeepWaitingForCard(error, paymentMethod)) {
                    // restart transaction
                    startTransaction();
                } else {
                    // stop reader
                    delegate.stopDevice();
                }
            }
        };

        this.executeCommand(Command.EMVTransactionStop, this);
    }

    private Boolean shouldReactToError(Error error) {
        if (error != null &&
                error.getErrorCode() == com.wepay.android.enums.ErrorCode.EMV_TRANSACTION_ERROR.getCode() &&
                error.getMessage().equalsIgnoreCase(ErrorCode.CardReaderNotConnected.toString())) {
            // CardReaderNotConnected error. No point stopping because card reader is no longer connected
            // This error is followed by an onDisconnected() message, which takes care of managing the flow
            return false;
        }

        return true;
    }

    private Error validateEMVResponse(Map<Parameter, Object> data) {
        Error error = null;
        ResponseCode responseCode = (ResponseCode) data.get(Parameter.ResponseCode);
        if (responseCode == ResponseCode.Error) {
            ErrorCode errorCode = (ErrorCode) data.get(Parameter.ErrorCode);
            // TODO: use [WPError errorWithCardReaderResponseData]

            if (errorCode == null) {
                error = Error.getEmvTransactionErrorWithMessage("unknown");
            } else if (errorCode.equals(ErrorCode.RSAKeyNotFound)
                    || errorCode.equals(ErrorCode.AIDNotInListOfMutuallySupportedAIDs)
                    || errorCode.equals(ErrorCode.NonEMVCardOrCardError)
                    || errorCode.equals(ErrorCode.NoMutuallySupportedAIDs)) {
                error = Error.getCardNotSupportedError();
            } else if (errorCode.equals(ErrorCode.CardExpired)) {
                error = Error.getCardReaderGeneralErrorWithMessage("Card has expired");
            } else if (errorCode.equals(ErrorCode.ApplicationBlocked)
                    || errorCode.equals(ErrorCode.CardBlocked)) {
                error = Error.getCardBlockedError();
            } else if (errorCode.equals(ErrorCode.TimeoutExpired)) {
                error = Error.getCardReaderTimeoutError();
            } else {
                // TODO: define more specific errors
                error = Error.getEmvTransactionErrorWithMessage(errorCode.toString());
            }
        } else {
            //no error, but response may still be invalid for us.

            // check cryptogram
            Command cmd = (Command) data.get(Parameter.Command);
            String cryptogramInformationData = (String) data.get(Parameter.CryptogramInformationData);

            boolean isCompleteTx = cmd.equals(Command.EMVCompleteTransaction);
            boolean isTxData = cmd.equals(Command.EMVTransactionData);
            boolean isCardDecline = CRYPTOGRAM_INFORMATION_DATA_00.equalsIgnoreCase(cryptogramInformationData);
            boolean isIssuerReachable = (this.authResponseCode != null);
            boolean isIssuerDecline = (this.authResponseCode != null) && !(this.authResponseCode.equalsIgnoreCase(AUTH_RESPONSE_CODE_ONLINE_APPROVE)); // non-null code that is not the approval code "00"

            if (isCompleteTx && !isIssuerReachable) {
                // could not reach issuer, should be declined, even if card approves
                // If an error was returned, use it. Otherwise create a generic error.
                error  = (this.authorizationError != null) ? this.authorizationError : Error.getIssuerUnreachableError();
            } else if (isCompleteTx && isIssuerDecline && isCardDecline) {
                // online decline, card confirmed decline
                error = Error.getCardDeclinedByIssuerError();
            } else if (isCompleteTx && isIssuerReachable && isCardDecline) {
                // online approved, declined by card
                error = Error.getDeclinedByCardError();

                // must issue reversal here
                this.shouldIssueReversal = true;
            } else if (isCompleteTx && !isIssuerReachable && isCardDecline) {
                // issuer unreachable, declined by card
                error = Error.getIssuerUnreachableError();
            } else if (isTxData && isCardDecline) {
                // offline declined
                error = Error.getDeclinedByCardError();
            }
        }

        return error;
    }

    @Override
    public void onProgress(ProgressMessage messageType, String additionalMessage) {
        Log.d("wepay_sdk", "Command progress: " + messageType.toString() + " - " + additionalMessage);

        switch (messageType) {
            case PleaseInsertCard:
                if (this.shouldReportSwipedEMVCard) {
                    // tell the app an emv card was swiped
                    this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.SHOULD_NOT_SWIPE_EMV_CARD);
                } else {
                    // inform delegate we are waiting for card
                    this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.WAITING_FOR_CARD);

                    // next time we get this progress message, it is because user is swiping EMV card
                    this.shouldReportSwipedEMVCard = true;
                }
                break;
            case PleaseRemoveCard:
                if (this.shouldReportCheckCardOrientation) {
                    this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.CHECK_CARD_ORIENTATION);
                }

                break;
            case CardInserted:
                this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.CARD_DIPPED);
                break;
            case SwipeDetected:
                this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.SWIPE_DETECTED);
                break;
            case ICCErrorSwipeCard:
                this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.CHIP_ERROR_SWIPE_CARD);
                this.isFallbackSwipe = true;
                break;
            case SwipeErrorReswipeMagStripe:
                this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.SWIPE_ERROR_SWIPE_AGAIN);
                break;
            case CommandSent:
                //do nothing
                break;
            default:
                // Do nothing on progress, react to the response when it comes
                Log.d("wepay_sdk", "unhandled progress message: " + messageType.toString() + " - " + additionalMessage);
                break;
        }
    }

    private boolean shouldExecuteMagicNumbers() {
        double[] magicNumbers = new double[]{ 21.61, 121.61, 22.61, 122.61, 24.61, 124.61, 25.61, 125.61 };
        boolean isMagicSuccessAmount = false;

        for (int i=0; i < magicNumbers.length; i++) {
            if (this.amount == magicNumbers[i]) {
                isMagicSuccessAmount = true;
                break;
            }
        }

        // YES, if not in production and amount is magic amount
        return (!this.config.getEnvironment().equalsIgnoreCase(Config.ENVIRONMENT_PRODUCTION) && isMagicSuccessAmount);
    }

    private void consumeAuthenticationData(String issuerAuthenticationData, String authResponseCode, String creditCardId) {
        this.issuerAuthenticationData = issuerAuthenticationData;
        this.authResponseCode = authResponseCode;
        this.creditCardId = creditCardId;

        if (this.shouldExecuteMagicNumbers()) {

            if (creditCardId == null) {
                this.creditCardId = "1234567890";
            }

            // inform external success
            this.reportAuthorizationSuccess(this.createAuthInfo(MAGIC_TC), null, this.paymentInfo);
            this.reactToError(null);

        } else {
            // complete the transaction
            this.executeCommand(Command.EMVCompleteTransaction, this);
        }
    }

    private String convertToEMVAmount(double amount){
        int intAmount = (int) (amount * 100);
        return String.format("%012d", intAmount);
    }

    private String convertToEmvCurrencyCode(String currencyCode) {
        String emvCurrencyCode = null;

        if (currencyCode.equals("USD")) {
            emvCurrencyCode = "0840";
        }

        return emvCurrencyCode;
    }

    private String convertResponseCodeToHexString(String responseCode) {
        char[] chars = responseCode.toCharArray();
        StringBuffer hex = new StringBuffer();
        for (int i = 0; i < chars.length; i++)
        {
            hex.append(Integer.toHexString((int) chars[i]));
        }
        return hex.toString();
    }
}