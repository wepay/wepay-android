package com.wepay.android.models;

import android.location.Address;
import android.support.test.runner.AndroidJUnit4;

import com.wepay.android.enums.PaymentMethod;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class UnitTestPaymentInfo {
    private static final String FIRST_NAME = "Donald";
    private static final String LAST_NAME = "Duck";
    private static final String EMAIL = "donald@wepay.com";
    private static final String PAYMENT_DESCRIPTION = "1111222233334444";
    private static final String ADDRESS_LINE_1 = "350 Convention Way";
    private static final String ADDRESS_LINE_2 = "#200";
    private static final String POSTAL_CODE = "94063";
    private static final String COUNTRY_CODE = "US";
    private PaymentMethod paymentMethod;
    private PaymentInfo paymentInfo;

    @Before
    public void setUp() {
        Address billingAddress = new Address(Locale.US);
        billingAddress.setAddressLine(0, ADDRESS_LINE_1);
        billingAddress.setAddressLine(1, ADDRESS_LINE_2);
        billingAddress.setPostalCode(POSTAL_CODE);
        billingAddress.setCountryCode(COUNTRY_CODE);

        Address shippingAddress = new Address(Locale.US);

        this.paymentMethod = PaymentMethod.DIP;
        this.paymentInfo = new PaymentInfo(FIRST_NAME, LAST_NAME, EMAIL, PAYMENT_DESCRIPTION, billingAddress, shippingAddress, paymentMethod, null, null, null, null, false);
    }

    @Test
    public void testToString() {
        String expected = "{\"firstName\":\"" + FIRST_NAME +
                "\",\"lastName\":\"" + LAST_NAME +
                "\",\"email\":\"" + EMAIL +
                "\",\"paymentDescription\":\"" + PAYMENT_DESCRIPTION +
                "\",\"paymentMethod\":\"" + this.paymentMethod +
                "\",\"billingAddress\":{\"addressLine1\":\"" + ADDRESS_LINE_1 +
                "\",\"addressLine2\":\"" + ADDRESS_LINE_2 +
                "\",\"postalCode\":\"" + POSTAL_CODE +
                "\",\"countryCode\":\"" + COUNTRY_CODE +
                "\"},\"shippingAddress\":{}}";

        String result = this.paymentInfo.toString();
        Assert.assertEquals(expected, result);
    }
}
