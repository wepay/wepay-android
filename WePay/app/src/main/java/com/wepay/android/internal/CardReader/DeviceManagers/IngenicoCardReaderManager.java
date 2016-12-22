package com.wepay.android.internal.CardReader.DeviceManagers;

import android.os.Handler;
import android.util.Log;

import com.roam.roamreaderunifiedapi.DeviceManager;
import com.roam.roamreaderunifiedapi.callback.DeviceResponseHandler;
import com.roam.roamreaderunifiedapi.callback.DeviceStatusHandler;
import com.roam.roamreaderunifiedapi.constants.Command;
import com.roam.roamreaderunifiedapi.constants.LanguageCode;
import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.roam.roamreaderunifiedapi.constants.ProgressMessage;
import com.roam.roamreaderunifiedapi.constants.ResponseCode;
import com.roam.roamreaderunifiedapi.constants.ResponseType;
import com.roam.roamreaderunifiedapi.data.CalibrationParameters;
import com.wepay.android.CalibrationHandler;
import com.wepay.android.CardReaderHandler;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.CurrencyCode;
import com.wepay.android.internal.CardReader.DeviceHelpers.CalibrationHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.IngenicoCardReaderDetector;
import com.wepay.android.internal.CardReader.DeviceHelpers.DipConfigHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.DipTransactionHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.ExternalCardReaderHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.TransactionDelegate;
import com.wepay.android.internal.CardReaderDirector.CardReaderRequest;
import com.wepay.android.internal.SharedPreferencesHelper;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class IngenicoCardReaderManager implements CardReaderManager,
                                                  DeviceResponseHandler,
                                                  TransactionDelegate,
                                                  DeviceStatusHandler,
        IngenicoCardReaderDetector.CardReaderDetectionDelegate {

    /** The Constant CONNECTION_TIME_SEC. */
    private static final int CONNECTION_TIME_SEC = 7;

    /** The Constant CONNECTION_TIME_MS. */
    private static final int CONNECTION_TIME_MS = CONNECTION_TIME_SEC * 1000;

    private static final int CARD_READER_TIMEOUT_INFINITE_SEC = 0;
    private static final int CARD_READER_TIMEOUT_DEFAULT_SEC = 60;

    /** Dummy values for reading a card's info */
    private static final BigDecimal READ_AMOUNT = new BigDecimal("1.00");
    private static final CurrencyCode READ_CURRENCY = CurrencyCode.USD;
    private static final int READ_ACCOUNT_ID = 12345;

    /** The reader should wait for card. */
    private boolean readerShouldWaitForCard;

    /** The reader is waiting for card. */
    private boolean readerIsWaitingForCard;

    /** The reader is connected. */
    private boolean isConnected;

    /** The config. */
    private Config config = null;

    /** The roam device manager */
    private DeviceManager roamDeviceManager = null;

    /** The external card reader helper. */
    private ExternalCardReaderHelper externalCardReaderHelper = null;

    /** The dip config helper. */
    private DipConfigHelper dipConfighelper = null;

    /** The dip transaction helper. */
    private DipTransactionHelper dipTransactionHelper = null;

    /** The CardReaderRequest type */
    private CardReaderRequest requestType = null;

    private int currPublicKeyIndex = 0;

    private Set<String> configuredDeviceHashes = new HashSet<String>();

    private String currentDeviceSerialNumber;

    /** The reader charge up timer. */
    private Handler readerChargeUpHandler = new Handler();

    /** The reader inform not connected timer. */
    private Handler readerInformNotConnectedHandler = new Handler();

    /** The reader charge up runnable. */
    private Runnable readerChargeUpRunnable = null;

    /** The reader inform not connected runnable. */
    private Runnable readerInformNotConnectedRunnable = null;

    private CalibrationHelper calibrationHelper = new CalibrationHelper();

    public static IngenicoCardReaderManager instantiate(Config config,
                                                        ExternalCardReaderHelper externalCardReaderHelper) {
        IngenicoCardReaderManager instance;

        IngenicoCardReaderDetector detector = new IngenicoCardReaderDetector();
        CalibrationParameters params;

        instance = new IngenicoCardReaderManager(config, externalCardReaderHelper);
        params = instance.calibrationHelper.getCalibrationParams(config);

        detector.findFirstAvailableCardReader(config, params, instance);

        return instance;
    }

    private IngenicoCardReaderManager(Config config,
                                      ExternalCardReaderHelper externalCardReaderHelper) {
        this.config = config;
        this.externalCardReaderHelper = externalCardReaderHelper;

        this.dipConfighelper = new DipConfigHelper(config);
        this.dipTransactionHelper = new DipTransactionHelper(config,
                                                             externalCardReaderHelper,
                                                             this,
                                                             this.dipConfighelper);
        configuredDeviceHashes = SharedPreferencesHelper.getConfiguredDevices(config.getContext());
        Log.d("wepay_sdk", "configuredDeviceHashes: " + configuredDeviceHashes);
    }

    /** CardReaderManager */

    @Override
    public void processCard() {
        Log.d("wepay_sdk", "processCard");
        this.stopWaitingForCard();
        this.readerShouldWaitForCard = true;
        this.checkAndWaitForEMVCard();
    }

    @Override
    public void startCardReader() {
        this.startWaitingForReader();

        // devices are only started for transactions, so we should wait for card
        this.readerShouldWaitForCard = true;
    }

    @Override
    public void stopCardReader() {
        endTransaction();

        // inform external
        externalCardReaderHelper.informExternalCardReader(CardReaderStatus.STOPPED);

        if (this.roamDeviceManager != null) {
            roamDeviceManager.release();
            roamDeviceManager = null;
        }
    }

    @Override
    public void calibrateDevice(final CalibrationHandler calibrationHandler) {
        this.calibrationHelper.calibrateCardReader(calibrationHandler, this.config);
    }

    @Override
    public void setCardReaderRequestType(CardReaderRequest cardReaderRequestType) {
        this.requestType = cardReaderRequestType;
    }

    @Override public Boolean isConnected() {
        return this.isConnected && roamDeviceManager != null;
    }

    /** TransactionDelegate */

    @Override
    public void onTransactionCompleted() {
        endTransaction();

        if (this.shouldStopCardReaderAfterTransaction()) {
            // stop reader
            this.stopCardReader();
        }
    }

    /** DeviceStatusHandler */

    @Override
    public void onConnected() {
        Log.d("wepay_sdk", "IngenicoCardReaderManager onConnected");

        if (this.roamDeviceManager != null) {
            Log.d("wepay_sdk", "roam devicemanager type is " + this.roamDeviceManager.getType().toString());
            Log.d("wepay_sdk", "is device ready? " + this.roamDeviceManager.isReady());
        }

        if (this.roamDeviceManager == null) {
            Log.d("wepay_sdk", "roamDeviceManager was null");
            this.startCardReader();
        } else if (!this.isConnected) {
            Log.d("wepay_sdk", "roam devicemanager type is " + this.roamDeviceManager.getType().toString());

            this.executeCommand(Command.ReadCapabilities, this);
            // cancel timer if it exists
            if (this.readerInformNotConnectedHandler != null) {
                this.readerInformNotConnectedHandler.removeCallbacks(this.readerInformNotConnectedRunnable);
            }
        }

        this.isConnected = true;
    }

    @Override
    public void onDisconnected() {
        Log.d("wepay_sdk", "IngenicoCardReaderManager onDisconnected");

        if (this.isConnected) {

            if (this.readerShouldWaitForCard) {
                // inform external and stop waiting for card
                this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.NOT_CONNECTED);
                this.stopWaitingForCard();
            } else if (!this.config.shouldStopCardReaderAfterTransaction()) {
                // inform external
                externalCardReaderHelper.informExternalCardReader(CardReaderStatus.NOT_CONNECTED);
            }

            this.isConnected = false;
        }
    }

    @Override
    public void onError(String message) {
        Log.d("wepay_sdk", "IngenicoCardReaderManager onError: " + message);

        if (this.isConnected) {
            if (this.readerIsWaitingForCard) {
                // inform delegate
                this.externalCardReaderHelper.informExternalCardReaderError(Error.getCardReaderStatusError(message));

                // stop device
                this.stopCardReader();
            }

            this.isConnected = false;
        }
    }

    private void endTransaction() {
        this.readerShouldWaitForCard = false;

        // stop waiting for card and cancel all pending notifications
        this.stopWaitingForCard();
    }

    private boolean shouldStopCardReaderAfterTransaction() {
        return this.config.shouldStopCardReaderAfterTransaction();
    }

    private String getCurrentDeviceConfigHash() {
        String currentDeviceConfigHash = "";
        try {
            currentDeviceConfigHash = this.dipConfighelper.generateDeviceConfigHash(currentDeviceSerialNumber);
        } catch (Exception e) {
            // Just logging the error and using an empty config hash as this failure shouldn't stop the emv flow
            // At worst we will be configuring the reader again
            Log.v("wepay_sdk", "Exception while generating device configuration hash " + e.getMessage());
        }

        Log.d("wepay_sdk", "currentDeviceConfigHash: " + currentDeviceConfigHash);
        return currentDeviceConfigHash;
    }


    private void checkAndWaitForEMVCard() {
        Log.d("wepay_sdk", "checkAndWaitForEMVCard");
        if (this.isConnected) {
            //report checking reader
            this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.CHECKING_READER);

            if (this.currentDeviceSerialNumber != null) {
                // check if we should reset device
                if (configuredDeviceHashes.contains(this.getCurrentDeviceConfigHash())) {
                    // config hash found
                    // ask external if we should reset anyway
                    this.externalCardReaderHelper.informExternalCardReaderResetCallback(new CardReaderHandler.CardReaderResetCallback() {
                        @Override
                        public void resetCardReader(boolean shouldReset) {
                            if (shouldReset) {
                                resetDevice();
                            } else {
                                setUserInterfaceOptions();
                            }
                        }
                    });
                } else {
                    // config hash not found
                    this.resetDevice();
                }
            } else {
                // read capabilities again
                this.executeCommand(Command.ReadCapabilities, this);
            }
        } else {
            this.startWaitingForReader();
        }
    }

    private void startWaitingForReader() {
        // cancel previous timer if it exists
        if (this.readerInformNotConnectedHandler != null) {
            this.readerInformNotConnectedHandler.removeCallbacks(this.readerInformNotConnectedRunnable);
        }

        // Wait a few seconds for the reader to be detected, otherwise announce not connected
        this.readerInformNotConnectedRunnable = new Runnable() {
            @Override
            public void run() {
                externalCardReaderHelper.informExternalCardReader(CardReaderStatus.NOT_CONNECTED);
            }
        };

        this.readerInformNotConnectedHandler.postDelayed(this.readerInformNotConnectedRunnable, CONNECTION_TIME_MS);
    }

    private void resetDevice() {
        Log.d("wepay_sdk", "resetDevice");
        this.currPublicKeyIndex = 0;
        this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.CONFIGURING_READER);
        this.executeCommand(Command.ClearAIDsList, this);
    }

    private void stopWaitingForCard()
    {
        Log.d("wepay_sdk", "stopWaitingForCard");
        this.readerIsWaitingForCard = false;

        // cancel not connected timer
        if (this.readerInformNotConnectedHandler != null) {
            this.readerInformNotConnectedHandler.removeCallbacks(this.readerInformNotConnectedRunnable);
        }

        // cancel checkAndWaitForSwipe timer
        if (this.readerChargeUpHandler != null) {
            this.readerChargeUpHandler.removeCallbacks(this.readerChargeUpRunnable);
        }

        // stop transaction if running
        if (this.roamDeviceManager != null) {
            this.roamDeviceManager.getTransactionManager().cancelLastCommand();
        }
    }

    private void fetchAuthInfoForTransaction() {
        if (requestType == CardReaderRequest.CARD_READER_FOR_READING) {
            // If we're just reading, use dummy info.
            Log.i("wepay_sdk", "Card request is for reading. Using dummy card info.");
            dipTransactionHelper.performEMVTransactionStartCommand(READ_AMOUNT, READ_CURRENCY.toString(), READ_ACCOUNT_ID, roamDeviceManager, requestType);
        } else {
            // Otherwise, we need to get the info according to the client.
            Log.d("wepay_sdk", "fetchAuthInfoForTransaction");
            this.externalCardReaderHelper.informExternalCardReaderAmountCallback(new CardReaderHandler.CardReaderTransactionInfoCallback() {

                @Override
                public void useTransactionInfo(BigDecimal amount, CurrencyCode currencyCode, long accountId) {
                    Log.d("wepay_sdk", String.format("got amount:%.2f, currency:%s, accountId:%d", amount, currencyCode.toString(), accountId));

                    // validate params
                    Error error = validateAuthInfo(amount, currencyCode, accountId);
                    if (error != null) {
                        externalCardReaderHelper.informExternalCardReaderError(error);
                    } else {
                        // kick-off transaction
                        readerIsWaitingForCard = true;

                        dipTransactionHelper.performEMVTransactionStartCommand(amount, currencyCode.toString(), accountId, roamDeviceManager, requestType);
                    }
                }
            });
        }
    }

    private Error validateAuthInfo(BigDecimal amount, CurrencyCode currencyCode, long accountId) {

        final CurrencyCode[] allowedCurrencyCodes = new CurrencyCode[] {CurrencyCode.USD};

        if (amount == null || new BigDecimal("0.99").compareTo(amount) > 0 || amount.scale() > 2) {
            // An empty amount was passed, or the amount is less than 1,
            // or the amount has more than two decimal places.
            return Error.getInvalidTransactionAmountError();
        } else if (!Arrays.asList(allowedCurrencyCodes).contains(currencyCode)) {
            // Invalid currency code has been passed.
            return Error.getInvalidTransactionCurrencyCodeError();
        } else if (accountId <= 0) {
            // Invalid account ID.
            return Error.getInvalidTransactionAccountIDError();
        }

        // no validation errors
        return null;
    }

    private void executeCommand(final Command cmd, final DeviceResponseHandler handler) {
        Log.d("wepay_sdk", "Executing " + cmd.toString());
        new Thread(new Runnable() {
            @Override
            public void run() {
                switch (cmd) {
                    case ReadCapabilities:
                        roamDeviceManager.getConfigurationManager().getDeviceCapabilities(handler);
                        break;
                    case ClearAIDsList:
                        roamDeviceManager.getConfigurationManager().clearAIDSList(
                                handler);
                        break;
                    case ClearPublicKeys:
                        roamDeviceManager.getConfigurationManager().clearPublicKeys(
                                handler);
                        break;
                    case SubmitAIDsList:
                        roamDeviceManager.getConfigurationManager().submitAIDList(
                                dipConfighelper.getAidsSet(), handler);
                        break;
                    case SubmitPublicKey:
                        roamDeviceManager.getConfigurationManager().submitPublicKey(
                                dipConfighelper.getPublicKeyList().get(currPublicKeyIndex), handler);
                        break;
                    case ConfigureAmountDOLData:
                        roamDeviceManager.getConfigurationManager().setAmountDOL(
                                dipConfighelper.getAmountDOLList(), handler);
                        break;
                    case ConfigureOnlineDOLData:
                        roamDeviceManager.getConfigurationManager().setOnlineDOL(
                                dipConfighelper.getOnlineDOLList(), handler);
                        break;
                    case ConfigureResponseDOLData:
                        roamDeviceManager.getConfigurationManager().setResponseDOL(
                                dipConfighelper.getResponseDOLList(), handler);
                        break;
                    case ConfigureUserInterfaceOptions:
                        roamDeviceManager.getConfigurationManager().setUserInterfaceOptions(
                                getCardReaderTimeout(), LanguageCode.ENGLISH, new Byte((byte) 0x00), new Byte((byte) 0x00), handler);
                    default:
                        break;

                }
            }
        }).start();
    }

    @Override
    public void onProgress(ProgressMessage message, String additionalMessage) {
        switch(message) {
            case CommandSent:
                //do nothing
                break;
            default:
                Log.d("wepay_sdk", "ignoring progress message: " + message.toString() + " - " + additionalMessage);
                // nothing to do here
        }
    }

    @Override
    public void onResponse(Map<Parameter, Object> data) {
        Log.d("wepay_sdk", "Command response: \n" + data.toString());

        Command cmd = (Command) data.get(Parameter.Command);
        ResponseCode responseCode = (ResponseCode) data.get(Parameter.ResponseCode);
        ResponseType responseType = (ResponseType) data.get(Parameter.ResponseType);

        if (responseCode == ResponseCode.Error) {
            this.handleRoamError(data);
        } else {
            switch (cmd) {
                case ReadCapabilities:
                    currentDeviceSerialNumber = (String) data
                            .get(Parameter.InterfaceDeviceSerialNumber);
                    isConnected = true;

                    if (readerShouldWaitForCard) {
                        // inform external and start waiting for card
                        externalCardReaderHelper.informExternalCardReader(CardReaderStatus.CONNECTED);
                        checkAndWaitForEMVCard();
                    } else if (!this.config.shouldStopCardReaderAfterTransaction()) {
                        // inform external
                        externalCardReaderHelper.informExternalCardReader(CardReaderStatus.CONNECTED);
                    }

                    break;
                case ClearAIDsList:
                    this.executeCommand(Command.ClearPublicKeys, this);
                    break;
                case ClearPublicKeys:
                    this.executeCommand(Command.SubmitAIDsList, this);
                    break;
                case SubmitAIDsList:
                    // submit first public key
                    this.submitPublicKeys();
                    break;
                case SubmitPublicKey:
                    // submit other public keys
                    currPublicKeyIndex++;
                    this.submitPublicKeys();
                    break;
                case ConfigureAmountDOLData:
                    this.executeCommand(Command.ConfigureOnlineDOLData, this);
                    break;
                case ConfigureOnlineDOLData:
                    this.executeCommand(Command.ConfigureResponseDOLData, this);
                    break;
                case ConfigureResponseDOLData:
                    this.setUserInterfaceOptions();
                    break;
                case ConfigureUserInterfaceOptions:
                    this.setExpectedDOLs();
                    break;
                default:
                    Log.d("wepay_sdk","Error: unexpected command " + cmd.toString());
                    break;
            }
        }
    }

    /** CardReaderDetectionDelegate */

    @Override
    public void onCardReaderManagerDetected(DeviceManager roamDeviceManager) {
        this.roamDeviceManager = roamDeviceManager;
        this.roamDeviceManager.registerDeviceStatusHandler(this);

        // Manually call onConnected() the first time to get everything going.
        this.onConnected();
    }

    @Override
    public void onCardReaderDetectionTimeout() {
        Log.d("wepay_sdk", "onCardReaderDetectionTimeout");
    }

    @Override
    public void onCardReaderDetectionFailed(String message) {
        Log.d("wepay_sdk", "device detection failed with message " + message);
    }

    private void setUserInterfaceOptions() {
        this.executeCommand(Command.ConfigureUserInterfaceOptions, this);
    }

    private void setExpectedDOLs() {
        this.roamDeviceManager.getConfigurationManager().setExpectedAmountDOL(this.dipConfighelper.getAmountDOLList());
        this.roamDeviceManager.getConfigurationManager().setExpectedOnlineDOL(this.dipConfighelper.getOnlineDOLList());
        this.roamDeviceManager.getConfigurationManager().setExpectedResponseDOL(this.dipConfighelper.getResponseDOLList());

        this.fetchAuthInfoForTransaction();
    }

    private void submitPublicKeys() {
        if (currPublicKeyIndex < dipConfighelper.getPublicKeyList().size()) {
            this.executeCommand(Command.SubmitPublicKey, this);
        } else {
            this.saveConfigHash();
            this.executeCommand(Command.ConfigureAmountDOLData, this);
        }
    }

    private void saveConfigHash() {
        try {
            String configuredDeviceHash = this.dipConfighelper.generateDeviceConfigHash(currentDeviceSerialNumber);
            configuredDeviceHashes.add(configuredDeviceHash);
            SharedPreferencesHelper.saveConfiguredDevices(config.getContext(), configuredDeviceHashes);
        } catch (Exception e) {
            // Just logging as this failure shouldn't stop the emv flow
            // At worst we will be configuring the reader again next time
            Log.v("wepay_sdk", "Exception while generating device configuration hash " + e.getMessage());
        }
    }

    private void handleRoamError(Map<Parameter, Object> data) {
        // Retry the command
        Command cmd = (Command) data.get(Parameter.Command);
        this.executeCommand(cmd, this);
    }

    private int getCardReaderTimeout() {
        // timeout depends on config
        if (config.shouldRestartTransactionAfterOtherErrors()) {
            // never time out
            return CARD_READER_TIMEOUT_INFINITE_SEC;
        } else {
            return CARD_READER_TIMEOUT_DEFAULT_SEC;
        }
    }
}
