package com.wepay.android.internal.CardReader.DeviceManagers;

import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by chaitanya.bagaria on 1/10/16.
 */
public interface DeviceManagerDelegate {

    public com.wepay.android.models.Error validatePaymentInfoForTokenization(PaymentInfo paymentInfo);

    public Error validateSwiperInfoForTokenization(Map<Parameter, Object> swiperInfo);

    public void handleSwipeResponse(Map<Parameter, Object> data, final String model, final BigDecimal amount, final String currencyCode, final long accountId, final boolean fallback, TransactionResponseHandler responseHandler);

    public void handlePaymentInfo(PaymentInfo paymentInfo, final String model, final BigDecimal amount, final String currencyCode, final long accountId, final boolean fallback, TransactionResponseHandler responseHandler);

    public void issueReversal(Long creditCardId, Long accountId, Map<Parameter, Object> cardInfo);

    public String sanitizePAN(String pan);

    public int getCardReaderTimeout();

    public void onCardReaderStopped();

    public interface TransactionResponseHandler {
        public void onSuccess(JSONObject response);
        public void onFailure(Error error);
        public void onFinish();
    }
}


