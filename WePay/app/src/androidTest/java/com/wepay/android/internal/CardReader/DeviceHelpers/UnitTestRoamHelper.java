package com.wepay.android.internal.CardReader.DeviceHelpers;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.roam.roamreaderunifiedapi.constants.Parameter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // We'll extract the values from the formatted response, which we expect to be in
        //  <KEY> : <VALUE> format.
        String entryPatternString = "\\S+\\s*:\\s*\\S+";
        Pattern formatPattern = Pattern.compile(entryPatternString);
        Matcher matcher = formatPattern.matcher(result);
        Map<String, String> checkResult = new HashMap<>(map.size());

        // Populate our check map with the parsed results.
        while (matcher.find()) {
            String key, value;
            String extractLine = matcher.group();
            Scanner entryScanner = new Scanner(extractLine).useDelimiter(":");

            // Extract the key and value from either side of the delimiter and put the entry into
            // our check map.
            key = entryScanner.next();
            value = entryScanner.next();
            checkResult.put(key, value);

            Log.d("unit test", "extractLine :" + extractLine);
        }

        // Assert that our parsed map has the entries we expect with the values we expect.
        Assert.assertEquals(TEST_PAN, checkResult.get(Parameter.PAN.toString()));
        Assert.assertEquals(TEST_NAME, checkResult.get(Parameter.CardHolderName.toString()));
    }


}
