package com.wepay.android.models;

import android.support.test.runner.AndroidJUnit4;

import com.roam.roamreaderunifiedapi.constants.Parameter;
import com.wepay.android.enums.ErrorCode;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class UnitTestError {
    private static final String ERROR_DOMAIN = "com.wepay.sdk";
    private static final String ERROR_DESCRIPTION = "test error";
    private static final String ERROR_CATEGORY = "error category";
    private Integer errorCode;
    private Exception innerException;
    private Error error;

    @Before
    public void setUp() {
        errorCode = ErrorCode.UNKNOWN_ERROR.getCode();
        innerException = new Exception();
        error = new Error(errorCode, ERROR_DOMAIN, ERROR_CATEGORY, ERROR_DESCRIPTION, innerException);
    }

    @Test
    public void testToString() {
        String expected = "{\"errorDomain\":\"" + ERROR_DOMAIN + "\",\"errorCategory\":\"" + ERROR_CATEGORY + "\",\"errorCode\":" + this.errorCode + ",\"errorDescription\":\"" + ERROR_DESCRIPTION + "\",\"innerException\":{\"stackTrace\":[],\"suppressedExceptions\":[]}}";

        String result = error.toString();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetErrorCategory() {
        Assert.assertEquals(ERROR_CATEGORY, error.getErrorCategory());
    }

    @Test
    public void testGetErrorDescription() {
        Assert.assertEquals(ERROR_DESCRIPTION, error.getErrorDescription());
    }

    @Test
    public void testGetInnerException() {
        Assert.assertEquals(innerException, error.getInnerException());
    }

    @Test
    public void testGetCardReaderInitializationError() {
        Error expected = new Error(ErrorCode.CARD_READER_INITIALIZATION_ERROR.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, "Failed to initialize card reader");
        Error result = Error.getCardReaderInitializationError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetCardReaderTimeoutError() {
        Error expected = new Error(ErrorCode.CARD_READER_TIME_OUT_ERROR.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, "Card reader timed out.");
        Error result = Error.getCardReaderTimeoutError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetCardReaderErrorWithMessage () {
        String errorMessage = "test error message";
        Error expected = new Error(ErrorCode.CARD_READER_STATUS_ERROR.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, errorMessage);
        Error result = Error.getCardReaderErrorWithMessage(errorMessage);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetInvalidCardDataError() {
        Error expected = new Error(ErrorCode.INVALID_CARD_DATA.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, "Invalid card data");
        Error result = Error.getInvalidCardDataError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetDeclinedByCardError() {
        Error expected = new Error(ErrorCode.DECLINED_BY_CARD.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, "The transaction was declined by the card");
        Error result = Error.getDeclinedByCardError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetCardBlockedError() {
        Error expected = new Error(ErrorCode.CARD_BLOCKED.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, "This card has been blocked");
        Error result = Error.getCardBlockedError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetIssuerUnreachableError() {
        Error expected = new Error(ErrorCode.ISSUER_UNREACHABLE.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, "The issuing bank could not be reached");
        Error result = Error.getIssuerUnreachableError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetNoDataReturnedError() {
        Error expected = new Error(ErrorCode.NO_DATA_RETURNED_ERROR.getCode(), Error.ERROR_DOMAIN_API, Error.ERROR_CATEGORY_API, "No data returned by the API.");;
        Error result = Error.getNoDataReturnedError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetCardDeclinedByIssuerError() {
        Error expected = new Error(ErrorCode.CARD_DECLINED_BY_ISSUER.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_SDK, "The transaction was declined by the issuer bank.");
        Error result = Error.getCardDeclinedByIssuerError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetTransactionInfoNotProvidedError() {
        Error expected = new Error(ErrorCode.TRANSACTION_INFO_NOT_PROVIDED.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_SDK, "Transaction info was not provided.");
        Error result = Error.getTransactionInfoNotProvidedError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetPaymentMethodCannotBeTokenizedError() {
        Error expected = new Error(ErrorCode.PAYMENT_METHOD_CANNOT_BE_TOKENIZED.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_SDK, "This payment method cannot be tokenized.");
        Error result = Error.getPaymentMethodCannotBeTokenizedError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetCardReaderGeneralError() {
        Error expected = new Error(ErrorCode.CARD_READER_GENERAL_ERROR.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, "Swipe failed due to: (a) uneven swipe speed, (b) fast swipe, (c) slow swipe, or (d) damaged card.");
        Error result = Error.getCardReaderGeneralError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetCardReaderUnknownError() {
        Error expected = new Error(ErrorCode.UNKNOWN_ERROR.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, "There was an unexpected error.");
        Error result = Error.getCardReaderUnknownError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetInvalidSignatureImageError() {
        Exception e = new Exception();
        Error expected = new Error(ErrorCode.INVALID_SIGNATURE_IMAGE_ERROR.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, "Invalid signature image provided.", e);
        Error result = Error.getInvalidSignatureImageError(e);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetNameNotFoundError() {
        Error expected = new Error(ErrorCode.NAME_NOT_FOUND_ERROR.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, "Name not found.");
        Error result = Error.getNameNotFoundError();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetErrorWithCardReaderResponseData1() {
        Error expected = Error.getCardReaderUnknownError();
        Error result = Error.getErrorWithCardReaderResponseData(null);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetErrorWithCardReaderResponseData2() {
        Error expected = Error.getCardReaderGeneralError();
        Map<Parameter, Object> data = new HashMap<>();
        data.put(Parameter.ErrorCode, "G4X_DECODE_SWIPE_FAIL");
        Error result = Error.getErrorWithCardReaderResponseData(data);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetCardReaderGeneralErrorWithMessage() {
        Error expected = new Error(ErrorCode.CARD_READER_GENERAL_ERROR.getCode(), Error.ERROR_DOMAIN_SDK, Error.ERROR_CATEGORY_CARD_READER, ERROR_DESCRIPTION);
        Error result = Error.getCardReaderGeneralErrorWithMessage(ERROR_DESCRIPTION);

        Assert.assertEquals(expected, result);
    }
}
