package com.wepay.android.internal.CardReader.DeviceManagers;

import android.os.Handler;
import android.util.Log;

import com.roam.roamreaderunifiedapi.DeviceManager;
import com.roam.roamreaderunifiedapi.callback.DeviceResponseHandler;
import com.roam.roamreaderunifiedapi.constants.Command;
import com.roam.roamreaderunifiedapi.constants.LanguageCode;
import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.roam.roamreaderunifiedapi.constants.ProgressMessage;
import com.roam.roamreaderunifiedapi.constants.ResponseCode;
import com.roam.roamreaderunifiedapi.constants.ResponseType;
import com.wepay.android.CardReaderHandler;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.CurrencyCode;
import com.wepay.android.enums.ErrorCode;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.internal.CardReader.DeviceHelpers.DipConfigHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.DipTransactionHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.ExternalCardReaderHelper;
import com.wepay.android.internal.SharedPreferencesHelper;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class RP350XManager implements com.wepay.android.internal.CardReader.DeviceManagers.DeviceManager, DeviceResponseHandler {

    /** The Constant RP350X_CONNECTION_TIME_SEC. */
    private static final int RP350X_CONNECTION_TIME_SEC = 7;

    /** The Constant RP350X_CONNECTION_TIME_MS. */
    private static final int RP350X_CONNECTION_TIME_MS = RP350X_CONNECTION_TIME_SEC * 1000;

    /** The reader should wait for card. */
    private boolean readerShouldWaitForCard;

    /** The reader is waiting for card. */
    private boolean readerIsWaitingForCard;

    /** The reader is connected. */
    private boolean readerIsConnected;

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

    /** The dip transaction helper. */
    private DipTransactionHelper dipTransactionHelper = null;

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


    public RP350XManager(Config config, DeviceManager roamDeviceManager, DeviceManagerDelegate managerDelegate, ExternalCardReaderHelper externalCardReaderHelper) {
        this.config = config;
        this.roamDeviceManager = roamDeviceManager;
        this.deviceManagerDelegate = managerDelegate;
        this.externalCardReaderHelper = externalCardReaderHelper;

        this.dipConfighelper = new DipConfigHelper(config);
        this.dipTransactionHelper = new DipTransactionHelper(config, this, managerDelegate, this.dipConfighelper);

        configuredDeviceHashes = SharedPreferencesHelper.getConfiguredDevices(config.getContext());
        Log.d("wepay_sdk", "configuredDeviceHashes: " + configuredDeviceHashes);
    }

    @Override
    public void processCard() {
        Log.d("wepay_sdk", "processCard");
        this.stopWaitingForCard();
        this.readerShouldWaitForCard = true;
        this.checkAndWaitForEMVCard();
    }

    @Override
    public void startDevice() {
        this.startWaitingForReader();

        // devices are only started for transactions, so we should wait for card
        this.readerShouldWaitForCard = true;
    }

    @Override
    public void stopDevice() {
        this.transactionCompleted();

        // inform external
        externalCardReaderHelper.informExternalCardReader(CardReaderStatus.STOPPED);

        if (this.roamDeviceManager != null) {
            roamDeviceManager.release();
            roamDeviceManager = null;
        }

        // inform the delegate that we've stopped.
        this.deviceManagerDelegate.onCardReaderStopped();
    }

    @Override
    public void transactionCompleted() {
        this.readerShouldWaitForCard = false;

        // stop waiting for card and cancel all pending notifications
        this.stopWaitingForCard();
    }

    @Override
    public boolean shouldRestartTransaction(Error error, PaymentMethod paymentMethod) {
        if (error != null) {
            // if the error code was a general error
            if (error.getErrorDomain().equals(Error.ERROR_DOMAIN_SDK) && error.getErrorCode().equals(ErrorCode.CARD_READER_GENERAL_ERROR.getCode())) {
                // return whether or not we're configured to restart on general error
                return this.config.shouldRestartTransactionAfterGeneralError();
            }
            // return whether or not we're configured to restart on other errors
            return this.config.shouldRestartTransactionAfterOtherErrors();
        } else if (paymentMethod.equals(PaymentMethod.SWIPE)) {
            // return whether or not we're configured to restart on successful swipe
            return this.config.shouldRestartTransactionAfterSuccess();
        } else {
            // don't restart on successful dip
            return false;
        }
    }

    @Override
    public boolean shouldStopCardReaderAfterTransaction() {
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
        final DeviceResponseHandler transactionResponseHandler = this;
        Log.d("wepay_sdk", "checkAndWaitForEMVCard");
        if (this.readerIsConnected) {
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

        this.readerInformNotConnectedHandler.postDelayed(this.readerInformNotConnectedRunnable, RP350X_CONNECTION_TIME_MS);
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
                    dipTransactionHelper.performEMVTransactionStartCommand(amount, currencyCode.toString(), accountId, roamDeviceManager, externalCardReaderHelper);
                }
            }
        });
    }

    private Error validateAuthInfo(BigDecimal amount, CurrencyCode currencyCode, long accountId) {

        final CurrencyCode[] allowedCurrencyCodes = new CurrencyCode[] {CurrencyCode.USD};

        if (amount == null || new BigDecimal("0.99").compareTo(amount) > 0 || amount.scale() > 2) {
            return Error.getInvalidTransactionInfoError();
        } else if (!Arrays.asList(allowedCurrencyCodes).contains(currencyCode)) {
            return Error.getInvalidTransactionInfoError();
        } else if (accountId <= 0) {
            return Error.getInvalidTransactionInfoError();
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
                                deviceManagerDelegate.getCardReaderTimeout(), LanguageCode.ENGLISH, new Byte((byte) 0x00), new Byte((byte) 0x00), handler);
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
                    readerIsConnected = true;

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

    @Override
    public void cardReaderConnected() {
        Log.d("wepay_sdk", "device connected");
        if (!this.readerIsConnected) {
            this.executeCommand(Command.ReadCapabilities, this);
            // cancel timer if it exists
            if (this.readerInformNotConnectedHandler != null) {
                this.readerInformNotConnectedHandler.removeCallbacks(this.readerInformNotConnectedRunnable);
            }
        }
    }

    @Override
    public void cardReaderDisconnected() {
        Log.d("wepay_sdk", "device disconnected");

        if (this.readerShouldWaitForCard && this.readerIsConnected) {
            // inform external and stop waiting for card
            this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.NOT_CONNECTED);
            this.stopWaitingForCard();
        } else if (!this.config.shouldStopCardReaderAfterTransaction()) {
            // inform external
            externalCardReaderHelper.informExternalCardReader(CardReaderStatus.NOT_CONNECTED);
        }

        this.readerIsConnected = false;
    }

    @Override
    public void cardReaderError(String message) {
        this.readerIsConnected = false;

        if (this.readerIsWaitingForCard) {
            // inform delegate
            this.externalCardReaderHelper.informExternalCardReaderError(Error.getCardReaderStatusError(message));

            // stop device
            this.stopDevice();
        }
    }
}


