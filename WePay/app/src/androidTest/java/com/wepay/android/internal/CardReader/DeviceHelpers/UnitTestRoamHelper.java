package com.wepay.android.internal.CardReader.DeviceHelpers;

import android.support.test.runner.AndroidJUnit4;

import com.roam.roamreaderunifiedapi.constants.Parameter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class UnitTestRoamHelper {
    private static final String TEST_PAN = "1111222233334444";
    private static final String TEST_NAME = "Duck/Donald";
    private Map<Parameter, Object> map;

    @Before
    public void setUp() {
        map = new HashMap<>();
        map.put(Parameter.CardHolderName, TEST_NAME);
        map.put(Parameter.PAN, TEST_PAN);
    }

    @Test
    public void testGetFirstName() {
        String result = RoamHelper.getFirstName(map);
        Assert.assertEquals("Donald", result);
    }

    @Test
    public void testGetLastName() {
        String result = RoamHelper.getLastName(map);
        Assert.assertEquals("Duck", result);
    }

    @Test
    public void testGetFullName() {
        String result = RoamHelper.getFullName(map);
        Assert.assertEquals("Donald Duck", result);
    }

    @Test
    public void testGetPaymentDescription() {
        String result = RoamHelper.getPaymentDescription(map);
        Assert.assertEquals(TEST_PAN, result);
    }

    @Test
    public void testGetFormattedResponseString() {
        String result = RoamHelper.getFormattedResponseString(map);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
                .append("PAN:").append(TEST_PAN).append("\n")
                .append("CardHolderName:").append(TEST_NAME).append("\n")
                .append("}");
        String expected = sb.toString();

        Assert.assertEquals(expected, result);
    }


}
