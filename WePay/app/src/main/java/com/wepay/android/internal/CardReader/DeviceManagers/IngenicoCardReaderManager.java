package com.wepay.android.internal.CardReader.DeviceManagers;

import android.os.Handler;
import android.util.Log;

import com.roam.roamreaderunifiedapi.DeviceManager;
import com.roam.roamreaderunifiedapi.RoamReaderUnifiedAPI;
import com.roam.roamreaderunifiedapi.callback.DeviceResponseHandler;
import com.roam.roamreaderunifiedapi.callback.DeviceStatusHandler;
import com.roam.roamreaderunifiedapi.constants.Command;
import com.roam.roamreaderunifiedapi.constants.DeviceType;
import com.roam.roamreaderunifiedapi.constants.ErrorCode;
import com.roam.roamreaderunifiedapi.constants.LanguageCode;
import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.roam.roamreaderunifiedapi.constants.ProgressMessage;
import com.roam.roamreaderunifiedapi.constants.ResponseCode;
import com.roam.roamreaderunifiedapi.constants.ResponseType;
import com.roam.roamreaderunifiedapi.data.CalibrationParameters;
import com.roam.roamreaderunifiedapi.data.Device;
import com.roam.roamreaderunifiedapi.emvreaders.MOBY3000DeviceManager;
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
import com.wepay.android.internal.mock.MockRoamDeviceManager;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

    /** The maximum number of times we'll attempt to get the card reader device's serial number */
    private static final int MAX_TRIES_FETCH_DEVICE_SERIAL_NUMBER = 3;

    /** The reader should perform card reader request operation. */
    private boolean readerShouldPerformOperation;

    /** The reader is waiting for card. */
    private boolean readerIsWaitingForCard;

    /** The reader is connected. */
    private boolean isConnected;

    /** The manager is searching for a reader. */
    private boolean isSearching;

    /** The stop command was issued. */
    private boolean isCardReaderStopped;

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

    /** The battery info command timer. */
    private Handler delayedOperationHandler = new Handler();

    /** The find card reader timer. */
    private Handler findReaderHandler = new Handler();

    /** The reader charge up runnable. */
    private Runnable readerChargeUpRunnable = null;

    /** The reader inform not connected runnable. */
    private Runnable readerInformNotConnectedRunnable = null;

    /** The reader inform not connected runnable. */
    private Runnable delayedOperationRunnable = null;

    /** The find reader runnable. */
    private Runnable findReaderRunnable = null;

    /** The discovered readers as provided in onCardReaderDevicesDetected(). */
    private List<Device> discoveredCardReaders = null;

    private CalibrationHelper calibrationHelper = new CalibrationHelper();

    private IngenicoCardReaderDetector detector = null;

    /** The name of the device that was successfully connected to */
    private String connectedDeviceName = null;

    /** How many attempts we've made to fetch the card reader device's serial number */
    private int deviceSerialNumberFetchCount = 0;

    public static IngenicoCardReaderManager instantiate(Config config,
                                                        ExternalCardReaderHelper externalCardReaderHelper) {

        return new IngenicoCardReaderManager(config, externalCardReaderHelper);
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
    public void processCardReaderRequest() {
        Log.d("wepay_sdk", "processCardReaderRequest");

        this.stopFindingCardReaders();
        this.stopWaitingForCard();
        this.stopPendingOperations();
        this.readerShouldPerformOperation = true;
		this.isCardReaderStopped = false;
		this.connectedDeviceName = null;
		this.deviceSerialNumberFetchCount = 0;
        if (this.requestType == CardReaderRequest.CARD_READER_FOR_BATTERY_LEVEL) {
            this.checkAndWaitForBatteryLevel();
        } else {
            this.checkAndWaitForEMVCard();
        }
    }

    @Override
    public void startCardReader() {
        this.readerShouldPerformOperation = true;
        this.isCardReaderStopped = false;
        this.startWaitingForReader();
        if (this.isSearching) {
            this.stopFindingCardReaders();
            this.findCardReadersAfterDelay(1000);
        } else {
            this.findCardReaders();
        }
    }

    @Override
    public void stopCardReader() {
        this.isCardReaderStopped = true;
        endOperation();
        stopFindingCardReaders();

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

    @Override
    public Boolean isSearching() {
        return this.isSearching;
    }

    /** TransactionDelegate */

    @Override
    public void onTransactionCompleted() {
        if (this.shouldStopCardReaderAfterOperation()) {
            // stop reader
            this.stopCardReader();
        } else {
            endOperation();
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

            if (this.connectedDeviceName != null) {
                SharedPreferencesHelper.rememberCardReader(this.connectedDeviceName, this.config.getContext());
            }

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

        if (this.isConnected && !this.isCardReaderStopped) {

            if (this.readerShouldPerformOperation) {
                // inform external and stop waiting for card
                this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.NOT_CONNECTED);
                this.stopWaitingForCard();
            } else if (!this.config.shouldStopCardReaderAfterOperation()) {
                // inform external
                externalCardReaderHelper.informExternalCardReader(CardReaderStatus.NOT_CONNECTED);
            }

            this.isConnected = false;
        }

        this.connectedDeviceName = null;
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

        this.connectedDeviceName = null;
    }

    private void endOperation() {
        this.readerShouldPerformOperation = false;

        // stop waiting for card and cancel all pending notifications
        this.stopWaitingForCard();
    }

    private boolean shouldStopCardReaderAfterOperation() {
        return this.config.shouldStopCardReaderAfterOperation();
    }

    private boolean shouldDelayOperation() {
        return this.dipTransactionHelper != null && this.dipTransactionHelper.isWaitingForCardRemoval;
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

    private void checkAndWaitForBatteryLevel() {
        if (this.isConnected()) {
            this.checkBatteryLevel();
        } else {
            this.startWaitingForReader();

            // Find card readers after a delay because we might have just stopped
            // card reader searches, and roam has problems if you cancel searches
            // and immediately synchronously search again (discoveryComplete gets
            // called for the cancelled searches).
            this.findCardReadersAfterDelay(1000);
        }
    }

    private void checkBatteryLevel() {
        final IngenicoCardReaderManager processor = this;
        this.delayedOperationHandler = new Handler();
        this.delayedOperationRunnable = new Runnable() {
            @Override
            public void run() {
                if (processor.isConnected()) {
                    if (processor.shouldDelayOperation()) {
                        processor.delayedOperationHandler.postDelayed(processor.delayedOperationRunnable, 1000);
                    } else {
                        processor.readerShouldPerformOperation = true;
                        processor.delayedOperationRunnable = null;
                        processor.roamDeviceManager.getBatteryStatus(processor);
                    }
                } else {
                    // We've been disconnected so we should start up the card reader
                    processor.startCardReader();
                }
            }
        };
        this.delayedOperationHandler.postDelayed(this.delayedOperationRunnable, 1000);
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
                                setExpectedDOLs();
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

            // Find card readers after a delay because we might have just stopped
            // card reader searches, and roam has problems if you cancel searches
            // and immediately synchronously search again (discoveryComplete gets
            // called for the cancelled searches).
            this.findCardReadersAfterDelay(1000);
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

    private void stopPendingOperations() {
        // stop battery info request if pending
        if (this.delayedOperationHandler != null && this.delayedOperationRunnable != null) {
            this.delayedOperationHandler.removeCallbacks(this.delayedOperationRunnable);
        }
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
        } else if (requestType == CardReaderRequest.CARD_READER_FOR_TOKENIZING) {
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
        } else {
            Log.e("wepay_sdk", "fetchAuthInfoForTransaction called with invalid card reader request type.");
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
                        break;
                    case BatteryInfo:
                        roamDeviceManager.getBatteryStatus(handler);
                        break;
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

        if (responseCode == ResponseCode.Error) {
            this.handleRoamError(data);
        } else {
            switch (cmd) {
                case ReadCapabilities:
                    currentDeviceSerialNumber = (String) data.get(Parameter.InterfaceDeviceSerialNumber);
                    deviceSerialNumberFetchCount++;

                    // Only proceed with completing the connection if we were able to get a serial number or we've given up.
                    if (currentDeviceSerialNumber != null || deviceSerialNumberFetchCount > MAX_TRIES_FETCH_DEVICE_SERIAL_NUMBER) {
                        isConnected = true;

                        if (readerShouldPerformOperation) {
                            // inform external and start waiting for card
                            externalCardReaderHelper.informExternalCardReader(CardReaderStatus.CONNECTED);
                            if (this.requestType == CardReaderRequest.CARD_READER_FOR_BATTERY_LEVEL) {
                                this.checkBatteryLevel();
                            } else {
                                this.checkAndWaitForEMVCard();
                            }
                        } else if (!this.config.shouldStopCardReaderAfterOperation()) {
                            // inform external
                            externalCardReaderHelper.informExternalCardReader(CardReaderStatus.CONNECTED);
                        }
                    } else if (deviceSerialNumberFetchCount < MAX_TRIES_FETCH_DEVICE_SERIAL_NUMBER) {
                        this.executeCommand(Command.ReadCapabilities, this);
                    } else {
                        this.resetDevice();
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
                    if (this.requestType == CardReaderRequest.CARD_READER_FOR_BATTERY_LEVEL) {
                        this.executeCommand(Command.BatteryInfo, this);
                    } else {
                        this.setExpectedDOLs();
                    }
                    break;
                case BatteryInfo:
                    int batteryLevel = (int) data.get(Parameter.BatteryLevel);
                    this.externalCardReaderHelper.informExternalBatteryLevelSuccess(batteryLevel);
                    if (this.config.shouldStopCardReaderAfterOperation()) {
                        // Stop if configured to
                        this.stopCardReader();
                    } else {
                        this.endOperation();
                    }
                    break;
                default:
                    Log.d("wepay_sdk","Error: unexpected command " + cmd.toString());
                    break;
            }
        }
    }

    private void findCardReaders() {
        this.isSearching = true;
        this.detector = new IngenicoCardReaderDetector();
        this.detector.findAvailableCardReaders(config, this);
	    this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.SEARCHING_FOR_READER);
    }

    private void findCardReadersAfterDelay(long delayMillis) {
        this.isSearching = true;
        this.findReaderRunnable = new Runnable() {
            @Override
            public void run() {
                findCardReaders();
            }
        };
        this.findReaderHandler.postDelayed(this.findReaderRunnable, delayMillis);
    }

    private void stopFindingCardReaders() {
        this.isSearching = false;
        if (this.detector != null) {
            this.detector.stopFindingCardReaders();
            this.detector = null;
        }

        if (this.findReaderHandler != null) {
            this.findReaderHandler.removeCallbacks(this.findReaderRunnable);
        }
    }

    /** CardReaderDetectionDelegate */
    @Override
    public void onCardReaderDevicesDetected(List<Device> devices) {
        this.isSearching = false;
        this.discoveredCardReaders = devices;
        final ArrayList<String> cardReaders = getNamesFromDevices(devices);
        String rememberedName;
        Device rememberedDevice;

        // Stop waiting for the card because now we are waiting for the partner.
        stopWaitingForCard();

        rememberedName = SharedPreferencesHelper.getRememberedCardReader(this.config.getContext());
        rememberedDevice = this.getDeviceByName(rememberedName, devices);

        if (rememberedDevice == null) {
            // No remembered device exists, or the remembered device wasn't detected --
            // ask what it should be from a list.
            Log.d("wepay_sdk", "No remembered device detected. Asking which device should be used.");
            this.externalCardReaderHelper.informExternalCardReaderSelectionCallback(new CardReaderHandler.CardReaderSelectionCallback() {
                @Override
                public void useCardReaderAtIndex(int selectedIndex) {
                    roamDeviceManager = getDeviceManagerFromDevices(discoveredCardReaders, selectedIndex);

                    if (roamDeviceManager != null) {
                        // selectedIndex validated by having a response from getDeviceManagerFromDevices()
                        Device selectedDevice = discoveredCardReaders.get(selectedIndex);
                        CalibrationParameters params = calibrationHelper.getCalibrationParams(config);

                        connectedDeviceName = cardReaders.get(selectedIndex);
                        initializeDeviceManager(roamDeviceManager, params, selectedDevice);
                    } else {
                        Error selectionError = Error.getInvalidCardReaderSelectionError();
                        externalCardReaderHelper.informExternalCardReaderError(selectionError);

                        if (config.shouldRestartTransactionAfterOtherErrors()) {
                            processCardReaderRequest();
                        }
                    }
                }
            }, getSanitizedNamesFromDevices(devices));
        } else {
            // Detected an existing remembered device, so use that.
            Log.d("wepay_sdk", "Detected remembered device " + rememberedName + ". Initializing this device");

            CalibrationParameters params = calibrationHelper.getCalibrationParams(config);
            this.roamDeviceManager = this.getDeviceManagerForDevice(rememberedDevice);

            if (roamDeviceManager.getClass() == MOBY3000DeviceManager.class) {
                roamDeviceManager.getConfigurationManager().activateDevice(rememberedDevice);
            }

            connectedDeviceName = rememberedName;
            initializeDeviceManager(roamDeviceManager, params, rememberedDevice);
        }
    }

    private Device getDeviceByName(String name, List<Device> devices) {
        Device foundDevice = null;

        for (Device d : devices) {
            if (d.getName().equals(name)) {
                foundDevice = d;
                break;
            }
        }

        return foundDevice;
    }

    private DeviceManager getDeviceManagerFromDevices(List<Device> devices, int index) {
        DeviceManager manager = null;

        if (-1 < index && index < devices.size()) {
            Device selectedDevice = devices.get(index);

            manager = this.getDeviceManagerForDevice(selectedDevice);
        }

        return manager;
    }

    private DeviceManager getDeviceManagerForDevice(Device device) {
        DeviceManager manager;

        if (device.getName().equals("AUDIOJACK") && config.getMockConfig().isUseMockCardReader()) {
            // Attempt Mock RP350 connection
            MockRoamDeviceManager mockManager = MockRoamDeviceManager.getDeviceManager();
            mockManager.setMockConfig(config.getMockConfig());
            manager = mockManager;
        } else if (device.getName().equals("AUDIOJACK") || device.getName().startsWith("RP350")) {
            // Attempt Real RP350 connection
            manager = RoamReaderUnifiedAPI.getDeviceManager(DeviceType.RP350x);
        } else {
            // Attempt MOBY3000 connection
            manager = RoamReaderUnifiedAPI.getDeviceManager(DeviceType.MOBY3000);
        }

        return manager;
    }

    private ArrayList<String> getNamesFromDevices(List<Device> devices) {
        ArrayList<String> names = new ArrayList<>(devices.size());
        for (Device d : devices) {
            names.add(d.getName());
        }
        return names;
    }

    private ArrayList<String> getSanitizedNamesFromDevices(List<Device> devices) {
        ArrayList<String> names = new ArrayList<>(devices.size());
        for (Device d : devices) {
            if (d.getName().startsWith("RP350")) {
                names.add("AUDIOJACK");
            } else {
                names.add(d.getName());
            }
        }
        return names;
    }

    private void initializeDeviceManager(DeviceManager roamDeviceManager, CalibrationParameters params, Device device) {
        if (!this.isConnected && this.readerShouldPerformOperation) {
            // If readerShouldPerformOperation and we're not currently connected, let's reset/start
            // the connection timer.
            this.startWaitingForReader();
        }

        if (roamDeviceManager.getClass() == MOBY3000DeviceManager.class) {
            roamDeviceManager.getConfigurationManager().activateDevice(device);
        }

        if (params != null) {
            roamDeviceManager.initialize(this.config.getContext(), this, params);
        } else {
            roamDeviceManager.initialize(this.config.getContext(), this);
        }
    }

    private void setUserInterfaceOptions() {
        this.executeCommand(Command.ConfigureUserInterfaceOptions, this);
    }

    private void setExpectedDOLs() {
        this.roamDeviceManager.getConfigurationManager().setExpectedAmountDOL(this.dipConfighelper.getAmountDOLList());
        this.roamDeviceManager.getConfigurationManager().setExpectedOnlineDOL(this.dipConfighelper.getOnlineDOLList());
        this.roamDeviceManager.getConfigurationManager().setExpectedResponseDOL(this.dipConfighelper.getResponseDOLList());

        final IngenicoCardReaderManager processor = this;
        this.delayedOperationHandler = new Handler();
        this.delayedOperationRunnable = new Runnable() {
            @Override
            public void run() {
                if (processor.isConnected()) {
                    if (processor.shouldDelayOperation()) {
                        processor.delayedOperationHandler.postDelayed(processor.delayedOperationRunnable, 1000);
                    } else {
                        processor.readerShouldPerformOperation = true;
                        processor.delayedOperationRunnable = null;
                        fetchAuthInfoForTransaction();
                    }
                } else {
                    // We've been disconnected so we should start up the card reader
                    processor.startCardReader();
                }
            }
        };
        this.delayedOperationHandler.postDelayed(this.delayedOperationRunnable, 1000);
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

        if (cmd == Command.BatteryInfo) {
            this.externalCardReaderHelper.informExternalBatteryLevelError(Error.getFailedToGetBatteryLevelError());
            if (this.config.shouldStopCardReaderAfterOperation()) {
                this.stopCardReader();
            }
        } else if (data.get(Parameter.ErrorCode) == ErrorCode.CardReaderNotConnected) {
            // Do nothing, the flow will continue when we get our onDisconnected response
        } else {
            this.executeCommand(cmd, this);
        }
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
