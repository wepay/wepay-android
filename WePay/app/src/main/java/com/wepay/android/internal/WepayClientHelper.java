package com.wepay.android.internal;

import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.models.PaymentInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chaitanya.bagaria on 1/11/16.
 */
public class WepayClientHelper {

    public static Map<String, Object> getCreditCardParams(PaymentInfo paymentInfo, String sessionId, String model, double amount, String currencyCode, long accountId, boolean fallback) {
        Map<String, Object> paramMap = null;
        Map<Parameter, Object> cardInfo = (Map<Parameter, Object>) paymentInfo.getCardReaderInfo();

        if (paymentInfo.getPaymentMethod() == PaymentMethod.DIP) {
            paramMap = WepayClientHelper.createDipSpecificRequestParams(cardInfo);
        } else {
            paramMap = WepayClientHelper.createSwipeSpecificRequestParams(cardInfo, amount, currencyCode, fallback);
        }

        paramMap.put("user_name", paymentInfo.getFullName());
        paramMap.put("encrypted_track", cardInfo.get(Parameter.EncryptedTrack));
        paramMap.put("account_id", accountId);
        paramMap.put("ksn", cardInfo.get(Parameter.KSN));
        paramMap.put("model", model);
        paramMap.put("account_id", accountId);

        paramMap.put("track_1_status", "0");
        paramMap.put("track_2_status", "0");

        paramMap.put("format_id", cardInfo.get(Parameter.FormatID));

        // add sessionId if present
        if (sessionId != null) {
            paramMap.put("device_token", sessionId);
        }

        String email = paymentInfo.getEmail();
        if (email != null) {
            paramMap.put("email", email);
        }

        return paramMap;
    }

    public static Map<String, Object> getReversalRequestParams(Long creditCardId, Long accountId, Map<Parameter, Object> map) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("emv", WepayClientHelper.createEMVTagParams(map));

        paramMap.put("credit_card_id", creditCardId);
        paramMap.put("account_id", accountId);

        return paramMap;
    }

    private static Map<String, Object> createSwipeSpecificRequestParams(Map<Parameter, Object> map, double amount, String currencyCode,boolean fallback) {
        HashMap<String, Object> paramMap = new HashMap<>();

        paramMap.put("amount", amount);
        paramMap.put("currency_code", currencyCode);
        paramMap.put("emv_fallback", fallback);

        return paramMap;
    }

    private static Map<String, Object> createDipSpecificRequestParams(Map<Parameter, Object> map) {
        HashMap<String, Object> paramMap = new HashMap<>();

        String formatId = (String) map.get(Parameter.FormatID);

        // Decryption fails if 77 or 78 is used, and works if 99 is used.
        // We can remove this workaround after Roam fixes the decryption service
        if (formatId == null || formatId.equalsIgnoreCase("77") || formatId.equalsIgnoreCase("78")) {
            formatId = "99";
        }

        paramMap.put("format_id", formatId);
        paramMap.put("emv", WepayClientHelper.createEMVTagParams(map));

        return paramMap;
    }

    private static Map<String, Object> createEMVTagParams(Map<Parameter, Object> map) {
        HashMap<String, Object> emvParamMap = new HashMap<>();

        emvParamMap.put("application_interchange_profile", map.get(Parameter.ApplicationInterchangeProfile));
        emvParamMap.put("terminal_verification_results", map.get(Parameter.TerminalVerificationResults));
        emvParamMap.put("transaction_date", map.get(Parameter.TransactionDate));
        emvParamMap.put("transaction_type", map.get(Parameter.TransactionType));
        emvParamMap.put("transaction_currency_code", map.get(Parameter.TransactionCurrencyCode));
        emvParamMap.put("amount_authorised", map.get(Parameter.AmountAuthorizedNumeric));
        emvParamMap.put("application_identifier", map.get(Parameter.ApplicationIdentifier));
        emvParamMap.put("issuer_application_data", map.get(Parameter.IssuerApplicationData));
        emvParamMap.put("terminal_country_code", map.get(Parameter.TerminalCountryCode));
        emvParamMap.put("application_cryptogram", map.get(Parameter.ApplicationCryptogram));
        emvParamMap.put("cryptogram_information_data", map.get(Parameter.CryptogramInformationData));
        emvParamMap.put("application_transaction_counter", map.get(Parameter.ApplicationTransactionCounter));
        emvParamMap.put("unpredictable_number", map.get(Parameter.UnpredictableNumber));

        ////////////////////////////
        // OPTIONAl PARAMS
        ////////////////////////////

        String panSequenceNumber = (String) map.get(Parameter.PANSequenceNumber);
        if (panSequenceNumber != null && !panSequenceNumber.equalsIgnoreCase("00") && !panSequenceNumber.equalsIgnoreCase("FF")) {
            // if panSequenceNumber is present and not 00 or FF, send it
            // this is a Vantiv-specific param name
            emvParamMap.put("card_sequence_terminal_number", panSequenceNumber);
        }

        String amountOther = (String) map.get(Parameter.AmountOtherNumeric);
        if (amountOther != null) {
            emvParamMap.put("amount_other", amountOther);
        }

        String applicationIdentifier = (String) map.get(Parameter.ApplicationIdentifier);
        if (applicationIdentifier != null) {
            emvParamMap.put("application_identifier_icc", applicationIdentifier);
        }

        String terminalCapabilities = (String) map.get(Parameter.TerminalCapabilities);
        if (terminalCapabilities != null) {
            emvParamMap.put("terminal_capabilities", terminalCapabilities);
        }

        String transactionStatusInformation = (String) map.get(Parameter.TransactionStatusInformation);
        if (transactionStatusInformation != null) {
            emvParamMap.put("transaction_status_information", transactionStatusInformation);
        }

        String terminalType = (String) map.get(Parameter.TerminalType);
        if (terminalType != null) {
            emvParamMap.put("terminal_type", terminalType);
        }

        String applicationLabel = (String) map.get(Parameter.ApplicationLabel);
        if (applicationLabel != null) {
            emvParamMap.put("application_label", applicationLabel);
        }

        return emvParamMap;
    }




}
