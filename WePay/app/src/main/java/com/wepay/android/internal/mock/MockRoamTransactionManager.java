package com.wepay.android.internal.mock;

import android.os.Handler;
import android.os.Looper;

import com.roam.roamreaderunifiedapi.TransactionManager;
import com.roam.roamreaderunifiedapi.callback.DeviceResponseHandler;
import com.roam.roamreaderunifiedapi.callback.DeviceStatusHandler;
import com.roam.roamreaderunifiedapi.constants.Command;
import com.roam.roamreaderunifiedapi.constants.ErrorCode;
import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.roam.roamreaderunifiedapi.constants.ProgressMessage;
import com.roam.roamreaderunifiedapi.constants.ResponseCode;
import com.roam.roamreaderunifiedapi.constants.ResponseType;
import com.roam.roamreaderunifiedapi.data.ApplicationIdentifier;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.models.MockConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockRoamTransactionManager implements TransactionManager{
    public ErrorCode mockCommandErrorCode = ErrorCode.UnknownError;

    private MockConfig mockConfig;
    private PaymentMethod paymentMethodToMock;
    private DeviceStatusHandler deviceStatusHandler;
    private static Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private boolean EMVAppAlreadySelected = false;
    private int selectedAppIndex = -1;
    private boolean isCommandCancelled = false;

    // AID's supported by card reader
    private String AID_MCRD = "A000000004";
    private String AID_VISA = "A000000003";
    private String AID_DISC = "A000000152";
    private String AID_AMEX = "A000000025";
    private String AID_JCB  = "A000000065";

    private static final String CARD_HOLDER_NAME = "LAST/FIRST";
    private static final String ENCRYPTED_TRACK = "85D0FFBF60286CB3069AA8F751CCC4835CA0E52630FD88261139A28BCF4E4E7FF2FBC0930EDE96D4F893611B62DF49BF249CE2378DE919E7C01FC13726BF314973207869BC1BC9FAACBA187A65B533D47F8D2650F8C55DB5840F5149C5EDDDEA0455E5798FB3285C455BA8D985327B7A";
    private static final String ENCRYPTED_TRACK_BAD = "xxxxFFBF60286CB3069AA8F751CCC4835CA0E52630FD88261139A28BCF4E4E7FF2FBC0930EDE96D4F893611B62DF49BF249CE2378DE919E7C01FC13726BF314973207869BC1BC9FAACBA187A65B533D47F8D2650F8C55DB5840F5149C5EDDDEA0455E5798FB3285C455BA8D985327B7A";
    private static final String KSN = "FFFFFF81000133400052";
    private static final String FORMAT_ID = "32";
    private static final String APPLICATION_INTERCHANGE_PROFILE = "5C00";
    private static final String TERMINAL_VERIFICATION_RESULTS = "0080008000";
    private static final String APPLICATION_IDENTIFIER = "A0000000031010";
    private static final String ISSUER_APPLICATION_DATA = "06010A03A00000";
    private static final String APPLICATION_CRYPTOGRAM = "D08AAF84DB5C5CE9";
    private static final String CRYPTOGRAM_INFORMATION_DATA = "80";
    private static final String APPLICATION_TRANSACTION_COUNTER = "0001";
    private static final String UNPREDICTABLE_NUMBER = "80C2328D";

    private String transactionDate;
    private String transactionType;
    private String transactionCurrencyCode;
    private String amountAuthorized;
    private String terminalCountryCode;

    private static final String[] PRIMARY_ACCOUNT_NUMBERS= new String[]{"0000111100001111", "0000111100002222", "0000111100003333", "0000111100004444"};


    public MockRoamTransactionManager(MockConfig mockConfig, DeviceStatusHandler deviceStatusHandler) {
        this.mockConfig = mockConfig;
        this.deviceStatusHandler = deviceStatusHandler;
    }

    public MockRoamTransactionManager setMockConfig(MockConfig mockConfig) {
        this.mockConfig = mockConfig;
        return this;
    }

    public MockRoamTransactionManager resetStates() {
        EMVAppAlreadySelected = false;
        selectedAppIndex = -1;
        transactionDate = null;
        transactionType = null;
        transactionCurrencyCode = null;
        amountAuthorized = null;
        terminalCountryCode = null;
        mockCommandErrorCode = ErrorCode.UnknownError;
        return this;
    }

    public MockRoamTransactionManager setDeviceStatusHandler(DeviceStatusHandler deviceStatusHandler) {
        this.deviceStatusHandler = deviceStatusHandler;
        return this;
    }

    @Override
    public void sendCommand(Map<Parameter, Object> map, DeviceResponseHandler deviceResponseHandler) {
        Command commandToExecute = (Command) map.get(Parameter.Command);
        isCommandCancelled = false;

        if (mockConfig != null) {
            paymentMethodToMock = mockConfig.getMockPaymentMethod();
        } else {
            paymentMethodToMock = PaymentMethod.SWIPE;
        }
        final DeviceResponseHandler responseHandler = deviceResponseHandler;

        final Map<Parameter, Object> res = new HashMap<>();
        res.put(Parameter.Command, commandToExecute);

        switch (commandToExecute) {
            case EMVStartTransaction:

                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        responseHandler.onProgress(ProgressMessage.CommandSent, null);
                        responseHandler.onProgress(ProgressMessage.PleaseInsertCard, null);

                        if (!isCommandCancelled) {
                            if (paymentMethodToMock.equals(PaymentMethod.DIP)) {
                                responseHandler.onProgress(ProgressMessage.CardInserted, null);
                            } else {
                                responseHandler.onProgress(ProgressMessage.SwipeDetected, null);
                            }
                        }
                    }
                });

                transactionDate = (String) map.get(Parameter.TransactionDate);
                transactionType = (String) map.get(Parameter.TransactionType);
                transactionCurrencyCode = (String) map.get(Parameter.TransactionCurrencyCode);
                amountAuthorized = (String) map.get(Parameter.AmountAuthorizedNumeric);
                terminalCountryCode = (String) map.get(Parameter.TerminalCountryCode);

                if (mockConfig.isCardReadFailure()) {
                    res.put(Parameter.ResponseCode, ResponseCode.Error);
                    res.put(Parameter.ErrorCode, this.mockCommandErrorCode);

                    break;
                }

                if (mockConfig.isMultipleEMVApplication() && !EMVAppAlreadySelected) {
                    // public ApplicationIdentifier(String AID, String PriorityIndex, String applicationLabel)
                    ApplicationIdentifier aid1 = new ApplicationIdentifier(AID_VISA, "1010", "Label 1");
                    ApplicationIdentifier aid2 = new ApplicationIdentifier(AID_VISA, "2010", "Label 2");
                    ApplicationIdentifier aid3 = new ApplicationIdentifier(AID_VISA, "2020", "Label 3");
                    ApplicationIdentifier aid4 = new ApplicationIdentifier(AID_VISA, "8010", "Label 4");
                    List<ApplicationIdentifier> applications = Arrays.asList(aid1, aid2, aid3, aid4);
                    res.put(Parameter.ResponseType, ResponseType.LIST_OF_AIDS);
                    res.put(Parameter.ListOfApplicationIdentifiers, applications);

                    EMVAppAlreadySelected = true;
                    break;
                }

                if (paymentMethodToMock.equals(PaymentMethod.SWIPE)) {
                    res.put(Parameter.ResponseType, ResponseType.MAGNETIC_CARD_DATA);

                    // Determine if want the tokenization to fail
                    if (mockConfig != null && mockConfig.isCardTokenizationFailure()) {
                        res.put(Parameter.EncryptedTrack, ENCRYPTED_TRACK_BAD);
                    } else {
                        res.put(Parameter.EncryptedTrack, ENCRYPTED_TRACK);
                    }

                    res.put(Parameter.KSN, KSN);
                    res.put(Parameter.FormatID, FORMAT_ID);
                    res.put(Parameter.PAN, PRIMARY_ACCOUNT_NUMBERS[0]);
                    res.put(Parameter.CardHolderName, CARD_HOLDER_NAME);
                } else if (paymentMethodToMock.equals(PaymentMethod.DIP)) {
                    res.put(Parameter.ResponseType, ResponseType.CONTACT_AMOUNT_DOL);
                    res.put(Parameter.ApplicationIdentifier, AID_VISA);
                }

                break;
            case EMVTransactionData:
                // only possible for PaymentMethod.DIP
                if (EMVAppAlreadySelected && selectedAppIndex != -1) {
                    res.put(Parameter.PAN, PRIMARY_ACCOUNT_NUMBERS[selectedAppIndex]);
                } else {
                    res.put(Parameter.PAN, PRIMARY_ACCOUNT_NUMBERS[0]);
                }
                res.put(Parameter.CardHolderName, CARD_HOLDER_NAME);
                // DIP also needs to return the following three parameters:
                // otherwise validatePaymentInfoForTokenization() will return error
                res.put(Parameter.EncryptedTrack, ENCRYPTED_TRACK);
                res.put(Parameter.KSN, KSN);
                res.put(Parameter.FormatID, FORMAT_ID);
                // other DIP specific parameters:
                res.put(Parameter.ApplicationInterchangeProfile, APPLICATION_INTERCHANGE_PROFILE);
                res.put(Parameter.TerminalVerificationResults, TERMINAL_VERIFICATION_RESULTS);
                res.put(Parameter.TransactionDate, transactionDate);
                res.put(Parameter.TransactionType, transactionType);
                res.put(Parameter.TransactionCurrencyCode, transactionCurrencyCode);
                res.put(Parameter.ApplicationIdentifier, APPLICATION_IDENTIFIER);
                res.put(Parameter.IssuerApplicationData, ISSUER_APPLICATION_DATA);
                res.put(Parameter.TerminalCountryCode, terminalCountryCode);
                res.put(Parameter.ApplicationCryptogram, APPLICATION_CRYPTOGRAM);
                res.put(Parameter.CryptogramInformationData, CRYPTOGRAM_INFORMATION_DATA);
                res.put(Parameter.ApplicationTransactionCounter, APPLICATION_TRANSACTION_COUNTER);
                res.put(Parameter.UnpredictableNumber, UNPREDICTABLE_NUMBER);
                res.put(Parameter.AmountAuthorizedNumeric, amountAuthorized);

                break;
            case EMVCompleteTransaction:
                break;
            case EMVTransactionStop:
                break;
            case EMVFinalApplicationSelection:
                String selectedAppID = (String) map.get(Parameter.ApplicationIdentifier);
                // last four digits of AID is PIX (Priority Index)
                String lastFourDigits = selectedAppID.substring(selectedAppID.length() - 4);
                if ("1010".equals(lastFourDigits)) {
                    selectedAppIndex = 0;
                } else if ("2010".equals(lastFourDigits)) {
                    selectedAppIndex = 1;
                } else if ("2020".equals(lastFourDigits)) {
                    selectedAppIndex = 2;
                } else {
                    selectedAppIndex = 3;
                }
                break;
            default:
                break;
        }

        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (!isCommandCancelled) {
                    responseHandler.onResponse(res);
                }
            }
        });
    }

    @Override
    public void cancelLastCommand() {
        this.isCommandCancelled = true;

        if (mainThreadHandler != null) {
            mainThreadHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void waitForMagneticCardSwipe(DeviceResponseHandler deviceResponseHandler) {

    }

    @Override
    public void stopWaitingForMagneticCardSwipe() {

    }

    @Override
    public void waitForCardRemoval(Integer cardRemovalTimeout, DeviceResponseHandler handler) {

    }

    private void runOnMainThread(Runnable runnable) {
        mainThreadHandler.post(runnable);
    }
}
