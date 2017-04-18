package com.wepay.android.internal.CardReader.DeviceManagers;

import com.wepay.android.CalibrationHandler;
import com.wepay.android.internal.CardReaderDirector.CardReaderRequest;

/**
 * Interface that defines the possible interactions with a card reader device.
 */

public interface CardReaderManager {
    void startCardReader();
    void stopCardReader();
    void calibrateDevice(final CalibrationHandler calibrationHandler);
    void processCardReaderRequest();
    void setCardReaderRequestType(CardReaderRequest cardReaderRequestType);
}
