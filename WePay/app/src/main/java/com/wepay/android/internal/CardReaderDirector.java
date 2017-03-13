package com.wepay.android.internal;

import android.content.Context;
import android.util.Log;

import com.wepay.android.AuthorizationHandler;
import com.wepay.android.BatteryLevelHandler;
import com.wepay.android.CalibrationHandler;
import com.wepay.android.CardReaderHandler;
import com.wepay.android.TokenizationHandler;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.internal.CardReader.DeviceHelpers.BatteryHelper;
import com.wepay.android.internal.CardReader.DeviceHelpers.ExternalCardReaderHelper;
import com.wepay.android.internal.CardReader.DeviceManagers.CardReaderManager;
import com.wepay.android.internal.CardReader.DeviceManagers.IngenicoCardReaderManager;
import com.wepay.android.models.Config;

/**
 * This class is detects the type of the connected reader and initiates swipe vs emv flow
 * accordingly.
 */
public class CardReaderDirector {

    private Config config = null;
    private CardReaderManager cardReaderManager = null;

    public enum CardReaderRequest{CARD_READER_FOR_READING, CARD_READER_FOR_TOKENIZING, CARD_READER_FOR_BATTERY_LEVEL}
    private CardReaderRequest cardReaderRequest = null;

    private ExternalCardReaderHelper externalCardReaderHelper = null;

    private String sessionId = null;

    public CardReaderDirector(Config config) {
        this.config = config;
        this.externalCardReaderHelper = new ExternalCardReaderHelper();
    }

    public void startCardReaderForReading(CardReaderHandler cardReaderHandler) {
        this.externalCardReaderHelper.setCardReaderHandler(cardReaderHandler);
        this.cardReaderRequest = CardReaderRequest.CARD_READER_FOR_READING;

        if (this.isCardReaderDeviceManagerConnected() || this.isCardReaderDeviceManagerSearching()) {
            this.cardReaderManager.setCardReaderRequestType(this.cardReaderRequest);
            this.cardReaderManager.processCardReaderRequest();
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

        if (this.isCardReaderDeviceManagerConnected() || this.isCardReaderDeviceManagerSearching()) {
            this.cardReaderManager.setCardReaderRequestType(this.cardReaderRequest);
            this.cardReaderManager.processCardReaderRequest();
        } else {
            instantiateCardReaderInstance();
        }
    }

    public void stopCardReader()
    {
        if (!this.isCardReaderDeviceManagerConnected() && !this.isCardReaderDeviceManagerSearching()) {
            Log.d("wepay_sdk", "CRHelper stopCardReader - no card reader connected");
            this.externalCardReaderHelper.informExternalCardReader(CardReaderStatus.STOPPED);
        } else {
            this.cardReaderManager.stopCardReader();
        }
    }

    public void calibrateCardReader(final CalibrationHandler calibrationHandler) {
        if (this.cardReaderManager == null) {
            instantiateCardReaderInstance();
        }

        this.cardReaderManager.calibrateDevice(calibrationHandler);
    }

    public void getCardReaderBatteryLevel(CardReaderHandler cardReaderHandler, BatteryLevelHandler batteryLevelHandler) {
        this.externalCardReaderHelper.setCardReaderHandler(cardReaderHandler);
        this.externalCardReaderHelper.setBatteryLevelHandler(batteryLevelHandler);
        this.cardReaderRequest = CardReaderRequest.CARD_READER_FOR_BATTERY_LEVEL;

        if (this.isCardReaderDeviceManagerConnected() || this.isCardReaderDeviceManagerSearching()) {
            this.cardReaderManager.setCardReaderRequestType(this.cardReaderRequest);
            this.cardReaderManager.processCardReaderRequest();
        } else {
            instantiateCardReaderInstance();
        }
    }

    public String getRememberedCardReader(Context context) {
        return SharedPreferencesHelper.getRememberedCardReader(context);
    }

    public void forgetRememberedCardReader(Context context) {
        SharedPreferencesHelper.forgetRememberedCardReader(context);
    }

    private void instantiateCardReaderInstance() {
        this.cardReaderManager = IngenicoCardReaderManager.instantiate(this.config,
                                                                                   this.externalCardReaderHelper);
        this.cardReaderManager.setCardReaderRequestType(this.cardReaderRequest);
        this.cardReaderManager.startCardReader();
    }

    private Boolean isCardReaderDeviceManagerConnected() {
        return this.cardReaderManager != null && this.cardReaderManager.isConnected();
    }

    private Boolean isCardReaderDeviceManagerSearching() {
        return this.cardReaderManager != null && this.cardReaderManager.isSearching();
    }
}
