package com.wepay.android.internal.CardReader.DeviceManagers;

import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.models.Error;
/**
 * Created by chaitanya.bagaria on 1/10/16.
 */
public interface DeviceManager {

    public void startDevice();
    public void stopDevice();
    public void processCard();
    public boolean shouldKeepWaitingForCard(Error error, PaymentMethod paymentMethod);
    public void cardReaderConnected();
    public void cardReaderDisconnected();
    public void cardReaderError(String message);
}
