package com.wepay.android.internal;

import android.support.test.runner.AndroidJUnit4;

import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.models.PaymentInfo;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class UnitTestWepayClientHelper {
    private static final BigDecimal AMOUNT = new BigDecimal("21.61");
    private static final String CURRENCY = "USD";
    private static final boolean FALLBACK = false;
    private static final String FORMAT_ID = "77";
    private static final Long CREDIT_CARD_ID = Long.valueOf("1234567890");
    private static final Long ACCOUNT_ID = Long.valueOf("946348693");

    private static final String APPLICATION_INTERCHANGE_PROFILE = "5C00";
    private static final String TERMINAL_VERIFICATION_RESULTS = "0080008000";
    private static final String TRANSACTION_DATE = "160606";
    private static final String TRANSACTION_TYPE = "00";
    private static final String TRANSACTION_CURRENCY_CODE = "0840";
    private static final String AMOUNT_AUTHORIZED_NUMERIC = "000000002461";
    private static final String APPLICATION_IDENTIFIER = "A0000000031010";
    private static final String ISSUER_APPLICATION_DATA = "06010A03A00000";
    private static final String TERMINAL_COUNTRY_CODE = "0840";
    private static final String APPLICATION_CRYPTOGRAM = "D08AAF84DB5C5CE9";
    private static final String CRYPTOGRAM_INFORMATION_DATA = "80";
    private static final String APPLICATION_TRANSACTION_COUNTER = "0001";
    private static final String UNPREDICTABLE_NUMBER = "80C2328D";
    private static final String PAN_SEQUENCE_NUMBER = "0001";
    private static final String AMOUNT_OTHER_NUMERIC = "000000000000";
    private static final String TERMINAL_CAPABILITIES = "E028C8";
    private static final String TRANSACTION_STATUS_INFORMATION = "000000";
    private static final String TERMINAL_TYPE = "22";
    private static final String APPLICATION_LABEL = "VISA CREDIT";
    private static final String ENCRYPTED_TRACK = "A0E2EBB42258803AAAD42FA637E81491B4B2F1690956801A41889A8AC0273277";
    private static final String KSN = "FFFFFF80030021E0030B";
    private static final String MODEL = "RP350X";
    private static final String SESSION_ID = "abcdefg123";
    private static final String EMAIL = "a@b.com";


    private Map<Parameter, Object> inputCardInfo;
    private Map<String, Object> expectedEMVTagParams;
    private PaymentInfo inputPaymentInfo;

    @Before
    public void setUp() {
        inputCardInfo = new HashMap<>();
        expectedEMVTagParams = new HashMap<>();

        inputCardInfo.put(Parameter.EncryptedTrack, ENCRYPTED_TRACK);
        inputCardInfo.put(Parameter.KSN, KSN);
        inputCardInfo.put(Parameter.FormatID, FORMAT_ID);

        inputCardInfo.put(Parameter.ApplicationInterchangeProfile, APPLICATION_INTERCHANGE_PROFILE);
        inputCardInfo.put(Parameter.TerminalVerificationResults, TERMINAL_VERIFICATION_RESULTS);
        inputCardInfo.put(Parameter.TransactionDate, TRANSACTION_DATE);
        inputCardInfo.put(Parameter.TransactionType, TRANSACTION_TYPE);
        inputCardInfo.put(Parameter.TransactionCurrencyCode, TRANSACTION_CURRENCY_CODE);
        inputCardInfo.put(Parameter.AmountAuthorizedNumeric, AMOUNT_AUTHORIZED_NUMERIC);
        inputCardInfo.put(Parameter.ApplicationIdentifier, APPLICATION_IDENTIFIER);
        inputCardInfo.put(Parameter.IssuerApplicationData, ISSUER_APPLICATION_DATA);
        inputCardInfo.put(Parameter.TerminalCountryCode, TERMINAL_COUNTRY_CODE);
        inputCardInfo.put(Parameter.ApplicationCryptogram, APPLICATION_CRYPTOGRAM);
        inputCardInfo.put(Parameter.CryptogramInformationData, CRYPTOGRAM_INFORMATION_DATA);
        inputCardInfo.put(Parameter.ApplicationTransactionCounter, APPLICATION_TRANSACTION_COUNTER);
        inputCardInfo.put(Parameter.UnpredictableNumber, UNPREDICTABLE_NUMBER);
        inputCardInfo.put(Parameter.PANSequenceNumber, PAN_SEQUENCE_NUMBER);
        inputCardInfo.put(Parameter.AmountOtherNumeric, AMOUNT_OTHER_NUMERIC);
        inputCardInfo.put(Parameter.ApplicationIdentifier, APPLICATION_IDENTIFIER);
        inputCardInfo.put(Parameter.TerminalCapabilities, TERMINAL_CAPABILITIES);
        inputCardInfo.put(Parameter.TransactionStatusInformation, TRANSACTION_STATUS_INFORMATION);
        inputCardInfo.put(Parameter.TerminalType, TERMINAL_TYPE);
        inputCardInfo.put(Parameter.ApplicationLabel, APPLICATION_LABEL);

        expectedEMVTagParams.put("application_interchange_profile", APPLICATION_INTERCHANGE_PROFILE);
        expectedEMVTagParams.put("terminal_verification_results", TERMINAL_VERIFICATION_RESULTS);
        expectedEMVTagParams.put("transaction_date", TRANSACTION_DATE);
        expectedEMVTagParams.put("transaction_type", TRANSACTION_TYPE);
        expectedEMVTagParams.put("transaction_currency_code", TRANSACTION_CURRENCY_CODE);
        expectedEMVTagParams.put("amount_authorised", AMOUNT_AUTHORIZED_NUMERIC);
        expectedEMVTagParams.put("application_identifier", APPLICATION_IDENTIFIER);
        expectedEMVTagParams.put("issuer_application_data", ISSUER_APPLICATION_DATA);
        expectedEMVTagParams.put("terminal_country_code", TERMINAL_COUNTRY_CODE);
        expectedEMVTagParams.put("application_cryptogram", APPLICATION_CRYPTOGRAM);
        expectedEMVTagParams.put("cryptogram_information_data", CRYPTOGRAM_INFORMATION_DATA);
        expectedEMVTagParams.put("application_transaction_counter", APPLICATION_TRANSACTION_COUNTER);
        expectedEMVTagParams.put("unpredictable_number", UNPREDICTABLE_NUMBER);
        expectedEMVTagParams.put("card_sequence_terminal_number", PAN_SEQUENCE_NUMBER);
        expectedEMVTagParams.put("amount_other", AMOUNT_OTHER_NUMERIC);
        expectedEMVTagParams.put("application_identifier_icc", APPLICATION_IDENTIFIER);
        expectedEMVTagParams.put("terminal_capabilities", TERMINAL_CAPABILITIES);
        expectedEMVTagParams.put("transaction_status_information", TRANSACTION_STATUS_INFORMATION);
        expectedEMVTagParams.put("terminal_type", TERMINAL_TYPE);
        expectedEMVTagParams.put("application_label", APPLICATION_LABEL);

        inputPaymentInfo = new PaymentInfo("Donald", "Duck", "paymentDes", PaymentMethod.SWIPE, inputCardInfo);
        inputPaymentInfo.addEmail(EMAIL);
    }

    @Test
    public void testGetCreditCardParams() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("amount", AMOUNT.doubleValue());
        expected.put("currency_code", CURRENCY);
        expected.put("emv_fallback", FALLBACK);
        expected.put("user_name", "Donald Duck");
        expected.put("encrypted_track", ENCRYPTED_TRACK);
        expected.put("account_id", ACCOUNT_ID);
        expected.put("ksn", KSN);
        expected.put("model", MODEL);
        expected.put("track_1_status", "0");
        expected.put("track_2_status", "0");
        expected.put("format_id", FORMAT_ID);
        expected.put("device_token", SESSION_ID);
        expected.put("email", EMAIL);

        Map<String, Object> result = WepayClientHelper.getCreditCardParams(inputPaymentInfo, SESSION_ID, MODEL, AMOUNT, CURRENCY, ACCOUNT_ID, false);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetReversalRequestParams() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("emv", expectedEMVTagParams);
        expected.put("credit_card_id", CREDIT_CARD_ID);
        expected.put("account_id", ACCOUNT_ID);

        Map<String, Object> result = WepayClientHelper.getReversalRequestParams(CREDIT_CARD_ID, ACCOUNT_ID, inputCardInfo);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testCreateSwipeSpecificRequestParams() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("amount", AMOUNT.doubleValue());
        expected.put("currency_code", CURRENCY);
        expected.put("emv_fallback", FALLBACK);

        Map<String, Object> result = WepayClientHelper.createSwipeSpecificRequestParams(null, AMOUNT, CURRENCY, FALLBACK);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testCreateDipSpecificRequestParams() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("format_id", "99");
        expected.put("emv", expectedEMVTagParams);

        Map<String, Object> result = WepayClientHelper.createDipSpecificRequestParams(inputCardInfo);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testCreateEMVTagParams() {
        Map<String, Object> result = WepayClientHelper.createEMVTagParams(inputCardInfo);

        Assert.assertEquals(expectedEMVTagParams, result);
    }
}
