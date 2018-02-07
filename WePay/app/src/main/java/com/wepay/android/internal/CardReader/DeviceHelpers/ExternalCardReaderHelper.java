package com.wepay.android.internal.CardReader.DeviceHelpers;

import com.wepay.android.AuthorizationHandler;
import com.wepay.android.BatteryLevelHandler;
import com.wepay.android.CardReaderHandler;
import com.wepay.android.TokenizationHandler;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.models.AuthorizationInfo;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

import java.util.ArrayList;

/**
 * Created by chaitanya.bagaria on 1/10/16.
 */
public class ExternalCardReaderHelper {

    private AuthorizationHandler authorizationHandler;
    private CardReaderHandler cardReaderHandler;
    private TokenizationHandler tokenizationHandler;
    private BatteryLevelHandler batteryLevelHandler;

    /**
     * Inform external card reader.
     *
     * @param status the status
     */
    public void informExternalCardReader(CardReaderStatus status) {
        if (this.cardReaderHandler != null) {
            this.cardReaderHandler.onStatusChange(status);
        }
    }

    public void informExternalAuthorizationApplications(CardReaderHandler.ApplicationSelectionCallback callback, ArrayList<String> applications) {
        if (this.cardReaderHandler != null) {
            this.cardReaderHandler.onEMVApplicationSelectionRequested(callback, applications);
        }
    }

    public void informExternalCardReaderSuccess(PaymentInfo paymentInfo) {
        if (this.cardReaderHandler != null) {
            this.cardReaderHandler.onSuccess(paymentInfo);
        }
    }

    public void informExternalCardReaderError(Error error) {
        if (this.cardReaderHandler != null) {
            this.cardReaderHandler.onError(error);
        }
    }

    public void informExternalCardReaderTokenizationSuccess(PaymentInfo paymentInfo, PaymentToken paymentToken) {
        if (this.tokenizationHandler != null) {
            this.tokenizationHandler.onSuccess(paymentInfo, paymentToken);
        }
    }

    public void informExternalCardReaderError(PaymentInfo paymentInfo, Error error) {
        if (this.tokenizationHandler != null) {
            this.tokenizationHandler.onError(paymentInfo, error);
        }
    }

    public void informExternalCardReaderResetCallback(CardReaderHandler.CardReaderResetCallback callback) {
        if (this.cardReaderHandler != null) {
            this.cardReaderHandler.onReaderResetRequested(callback);
        }
    }

    public void informExternalCardReaderAmountCallback(CardReaderHandler.CardReaderTransactionInfoCallback callback) {
        if (this.cardReaderHandler != null) {
            this.cardReaderHandler.onTransactionInfoRequested(callback);
        }
    }

    public void informExternalCardReaderEmailCallback(CardReaderHandler.CardReaderEmailCallback callback) {
        if (this.cardReaderHandler != null) {
            this.cardReaderHandler.onPayerEmailRequested(callback);
        }
    }

    public void informExternalCardReaderSelectionCallback(CardReaderHandler.CardReaderSelectionCallback callback, ArrayList<String> readers) {
        if (this.cardReaderHandler != null) {
            this.cardReaderHandler.onCardReaderSelection(callback, readers);
        }
    }

    public void informExternalAuthorizationSuccess(PaymentInfo paymentInfo, AuthorizationInfo authInfo) {
        if (this.authorizationHandler != null) {
            this.authorizationHandler.onAuthorizationSuccess(paymentInfo, authInfo);
        }
    }

    public void informExternalAuthorizationError(PaymentInfo paymentInfo, Error error) {
        if (this.authorizationHandler != null) {
            this.authorizationHandler.onAuthorizationError(paymentInfo, error);
        }
    }

    public void informExternalBatteryLevelSuccess(int batteryLevel) {
        if (this.batteryLevelHandler != null) {
            this.batteryLevelHandler.onBatteryLevel(batteryLevel);
        }
    }

    public void informExternalBatteryLevelError(Error error) {
        if (this.batteryLevelHandler != null) {
            this.batteryLevelHandler.onBatteryLevelError(error);
        }
    }

    public void setAuthorizationHandler(AuthorizationHandler authorizationHandler) {
        this.authorizationHandler = authorizationHandler;
    }

    public void setCardReaderHandler(CardReaderHandler cardReaderHandler) {
        this.cardReaderHandler = cardReaderHandler;
    }

    public TokenizationHandler getTokenizationHandler() {
        return tokenizationHandler;
    }

    public void setTokenizationHandler(TokenizationHandler tokenizationHandler) {
        this.tokenizationHandler = tokenizationHandler;
    }

    public BatteryLevelHandler getBatteryLevelHandler() {
        return batteryLevelHandler;
    }

    public void setBatteryLevelHandler(BatteryLevelHandler batteryLevelHandler) {
        this.batteryLevelHandler = batteryLevelHandler;
    }
}
